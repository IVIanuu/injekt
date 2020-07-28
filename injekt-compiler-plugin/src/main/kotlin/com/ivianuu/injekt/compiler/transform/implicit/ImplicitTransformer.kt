
/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import com.ivianuu.injekt.compiler.isReaderLambdaInvoke
import com.ivianuu.injekt.compiler.jvmNameAnnotation
import com.ivianuu.injekt.compiler.readableName
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ImplicitTransformer(
    pluginContext: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper
) : AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val remappedTransformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedClasses = mutableSetOf<IrClass>()

    private val globalNameProvider = NameProvider()

    fun getTransformedFunction(function: IrFunction) =
        transformFunctionIfNeeded(function)

    fun getFunctionForContext(context: IrClass): IrFunction? {
        val functionName = context.getAnnotation(InjektFqNames.Name)
            ?.getValueArgument(0)
            ?.let { it as IrConst<String> }
            ?.value
            ?: return null

        val functionFqName = functionName
            .replaceAfter("__", "")
            .replace("__", "")

        return try {
            pluginContext.referenceFunctions(FqName(functionFqName))
        } catch (e: Exception) {
            emptyList()
        }
            .map { it.owner }
            .map { getTransformedFunction(it) }
            .firstOrNull {
                it.getContext()
                    ?.let { symbolRemapper.getReferencedClass(it.symbol) } ==
                        symbolRemapper.getReferencedClass(context.symbol)
            }
    }

    override fun lower() {
        module.transformChildrenVoid(Transformer())

        (transformedClasses
            .map {
                it.getReaderConstructor(pluginContext.bindingContext)!!
            } + transformedFunctions.values)
            .filterNot { it.isExternalDeclaration() }
            .map { it.getContext()!! }
            .forEach { readerContext ->
                val parent = readerContext.parent as IrDeclarationContainer
                if (readerContext !in parent.declarations) {
                    parent.addChild(readerContext)
                }
            }

        module.rewriteTransformedFunctionRefs()

        module.acceptVoid(symbolRemapper)

        val typeRemapper =
            ReaderTypeRemapper(
                pluginContext,
                symbolRemapper
            )
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            pluginContext,
            symbolRemapper,
            typeRemapper
        ).also { typeRemapper.deepCopy = it }
        module.files.forEach {
            it.transformChildren(
                transformer,
                null
            )
        }

        transformedFunctions.forEach { (original, transformed) ->
            val remapped = symbolRemapper.getReferencedFunction(transformed.symbol).owner
            remappedTransformedFunctions[original] = remapped
            remappedTransformedFunctions[transformed] = remapped
        }
    }

    private inner class Transformer : IrElementTransformerVoid() {
        override fun visitClass(declaration: IrClass): IrStatement =
            super.visitClass(transformClassIfNeeded(declaration))

        override fun visitFunction(declaration: IrFunction): IrStatement =
            super.visitFunction(transformFunctionIfNeeded(declaration))
    }

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor(pluginContext.bindingContext)

        if (!clazz.isMarkedAsImplicit(pluginContext.bindingContext) && readerConstructor == null) return clazz

        if (readerConstructor == null) return clazz

        transformedClasses += clazz

        if (readerConstructor.valueParameters.any { it.name.asString() == "_context" })
            return clazz

        val existingSignature = getExternalReaderContext(clazz)

        if (clazz.isExternalDeclaration() || existingSignature != null) {
            val readerContext = getExternalReaderContext(clazz)!!
            readerConstructor.addContextParameter(readerContext)
            return clazz
        }

        lateinit var contextField: IrField
        lateinit var contextParameter: IrValueParameter

        transformDeclaration(
            owner = clazz,
            ownerFunction = readerConstructor,
            onContextParameterCreated = {
                contextParameter = it
                contextField = clazz.addField(
                    fieldName = "_context",
                    fieldType = it.type
                )
            },
            provideContext = { _, scopes ->
                if (scopes.none { it.irElement == readerConstructor }) {
                    irGetField(
                        irGet(scopes.thisOfClass(clazz)!!),
                        contextField
                    )
                } else {
                    irGet(contextParameter)
                }
            }
        )

        readerConstructor.body = DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            irBlockBody {
                readerConstructor.body?.statements?.forEach {
                    +it
                    if (it is IrDelegatingConstructorCall) {
                        +irSetField(
                            irGet(clazz.thisReceiver!!),
                            contextField,
                            irGet(contextParameter)
                        )
                    }
                }
            }
        }

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function is IrConstructor) {
            return if (function.canUseImplicits(pluginContext.bindingContext)) {
                transformClassIfNeeded(function.constructedClass)
                    .getReaderConstructor(pluginContext.bindingContext)!!
            } else function
        }

        remappedTransformedFunctions[function]?.let { return it }
        if (function in remappedTransformedFunctions.values) return function
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.canUseImplicits(pluginContext.bindingContext)) return function

        if (function.valueParameters.any { it.name.asString() == "_context" }) {
            transformedFunctions[function] = function
            return function
        }

        val existingContext = getExternalReaderContext(function)

        if (function.isExternalDeclaration() || existingContext != null) {
            val transformedFunction = function.copyAsReader()
            transformedFunctions[function] = transformedFunction

            if (!transformedFunction.isGiven) {
                val context = getExternalReaderContext(transformedFunction)!!
                transformedFunction.addContextParameter(context)
            }

            return transformedFunction
        }

        val transformedFunction = function.copyAsReader()
        transformedFunctions[function] = transformedFunction

        transformDeclaration(
            owner = transformedFunction,
            ownerFunction = transformedFunction,
            remapType = { it.remapTypeParameters(function, transformedFunction) },
            provideContext = { valueParameter, _ -> irGet(valueParameter) }
        )

        return transformedFunction
    }

    private fun <T> transformDeclaration(
        owner: T,
        ownerFunction: IrFunction,
        remapType: (IrType) -> IrType = { it },
        onContextParameterCreated: (IrValueParameter) -> Unit = {},
        provideContext: IrBuilderWithScope.(IrValueParameter, List<ScopeWithIr>) -> IrExpression
    ) where T : IrDeclarationWithName, T : IrDeclarationWithVisibility, T : IrDeclarationParent, T : IrTypeParametersContainer {
        val givenCalls = mutableListOf<IrCall>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        val componentsByLambdaValueParameter =
            mutableMapOf<IrValueParameter, MutableSet<IrType>>()

        owner.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()
            private val runReaderStack = mutableListOf<IrCall>()

            init {
                if (owner is IrFunction) functionStack += owner
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isMarkedAsImplicit(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val isRunReader = expression.symbol.descriptor.fqNameSafe.asString() ==
                        "com.ivianuu.injekt.runReader"
                if (isRunReader) runReaderStack.push(expression)
                return super.visitCall(expression)
                    .also { if (isRunReader) runReaderStack.pop() }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                if (expression !is IrCall &&
                    expression !is IrConstructorCall &&
                    expression !is IrDelegatingConstructorCall
                ) return super.visitFunctionAccess(expression)

                if (expression.isReaderLambdaInvoke()) {
                    if (expression.isReaderLambdaInvoke() && runReaderStack.isNotEmpty()) {
                        expression as IrCall

                        val lambdaValueParameter = expression.dispatchReceiver!!
                            .let { it as? IrGetValue }
                            ?.symbol
                            ?.let { valueParameterSymbol ->
                                ownerFunction.valueParameters
                                    .singleOrNull { it.symbol == valueParameterSymbol }
                            }

                        if (lambdaValueParameter != null) {
                            componentsByLambdaValueParameter.getOrPut(lambdaValueParameter) {
                                mutableSetOf()
                            } += runReaderStack.last().extensionReceiver!!.type
                        }
                    }
                }

                if (functionStack.isNotEmpty() &&
                    functionStack.lastOrNull() != owner
                ) return super.visitFunctionAccess(expression)

                if (expression.symbol.owner.canUseImplicits(pluginContext.bindingContext) ||
                    expression.isReaderLambdaInvoke()
                ) {
                    if (expression is IrCall && expression.symbol.owner.isGiven) {
                        givenCalls += expression
                    } else {
                        readerCalls += expression
                    }
                }

                return super.visitFunctionAccess(expression)
            }
        })

        val parentFunction =
            if (owner.visibility == Visibilities.LOCAL && owner.parent is IrFunction)
                owner.parent as IrFunction else null

        val context = buildClass {
            kind = ClassKind.INTERFACE
            name = globalNameProvider.allocateForGroup(
                getJoinedName(
                    owner.getPackageFragment()!!.fqName,
                    owner.descriptor.fqNameSafe
                        .parent().child(owner.name.asString().asNameId())
                ).asString() + "Context"
            ).asNameId()
            visibility = Visibilities.INTERNAL
        }.apply {
            parent = owner.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(owner)
            parentFunction?.let { copyTypeParametersFrom(it) }

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.name.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(owner.uniqueName())
                    )
                }
            }
        }

        val contextParameter = ownerFunction.addContextParameter(context)
        onContextParameterCreated(contextParameter)

        val genericFunctionMap = mutableListOf<Pair<IrFunction, IrFunction>>()

        fun addFunctionsFromGenericContext(
            genericContext: IrClass,
            typeArguments: List<IrType>
        ) {
            context.superTypes += genericContext.superTypes
            genericContext.functions
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .filterNot { it is IrConstructor }
                .forEach { genericContextFunction ->
                    genericFunctionMap += genericContextFunction to context.addFunction {
                        name = genericContextFunction.returnType.readableName()
                        returnType = genericContextFunction.returnType
                            .substitute(genericContext.typeParameters
                                .map { it.symbol }
                                .zip(typeArguments).toMap()
                            )
                            .let(remapType)
                            .remapTypeParameters(owner, context)
                            .let {
                                if (parentFunction != null) {
                                    it.remapTypeParameters(parentFunction, context)
                                } else it
                            }
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = context.thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()
                    }
                }
        }

        fun addSubcontext(
            subcontext: IrClass,
            typeArguments: List<IrType>
        ) {
            context.superTypes += subcontext.defaultType.typeWith(*typeArguments.toTypedArray())
        }

        fun handleSubcontext(
            subcontext: IrClass,
            typeArguments: List<IrType>
        ) {
            if (subcontext.typeParameters.isNotEmpty()) {
                addFunctionsFromGenericContext(subcontext, typeArguments)
            } else {
                addSubcontext(subcontext, typeArguments)
            }
        }

        val providerFunctionByGivenCall = givenCalls.associateWith { givenCall ->
            context.addFunction {
                val type = givenCall.getRealGivenType()
                    .let(remapType)
                    .remapTypeParameters(owner, context)
                    .let {
                        if (parentFunction != null) {
                            it.remapTypeParameters(parentFunction, context)
                        } else it
                    }
                name = type.readableName()
                returnType = type
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = context.thisReceiver?.copyTo(this)
                addMetadataIfNotLocal()
            }
        }

        readerCalls
            .forEach { call ->
                if (!call.isReaderLambdaInvoke()) {
                    val callContext = transformFunctionIfNeeded(call.symbol.owner).getContext()!!
                    handleSubcontext(callContext, call.typeArguments)
                } else {
                    /*val transformedLambda =
                        transformFunctionIfNeeded(lambdaSource.getFunctionArgument())
                    val lambdaContext = getContextForFunction(transformedLambda)
                    handleSubcontext(lambdaContext, call, emptyList())*/
                }
                call.getReaderLambdaArguments()
                    .forEach { expr ->
                        val transformedLambda =
                            transformFunctionIfNeeded(expr.getFunctionFromArgument())
                        val lambdaContext = transformedLambda.getContext()!!
                        handleSubcontext(lambdaContext, emptyList())
                    }
            }

        rewriteCalls(
            owner = owner,
            givenCalls = givenCalls,
            readerCalls = readerCalls,
            providerFunctionByGivenCall = providerFunctionByGivenCall,
            genericFunctionMap = genericFunctionMap,
            provideContext = { scopes ->
                provideContext(this, contextParameter, scopes)
            }
        )
    }

    private fun IrFunction.addContextParameter(context: IrClass): IrValueParameter {
        return addValueParameter(
            name = "_context",
            type = context.typeWith(
                typeParameters.map { it.defaultType }
            )
        )
    }

    private fun IrFunctionAccessExpression.getRealGivenType(): IrType {
        if (!symbol.owner.isGiven) return type

        val arguments = (getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        return when {
            arguments.isNotEmpty() -> pluginContext.tmpFunction(arguments.size)
                .typeWith(arguments.map { it.type } + type)
            else -> type
        }
    }

    private fun <T> rewriteCalls(
        owner: T,
        givenCalls: List<IrCall>,
        readerCalls: List<IrFunctionAccessExpression>,
        genericFunctionMap: List<Pair<IrFunction, IrFunction>>,
        providerFunctionByGivenCall: Map<IrCall, IrFunction>,
        provideContext: IrBuilderWithScope.(List<ScopeWithIr>) -> IrExpression
    ) where T : IrDeclarationWithName, T : IrDeclarationParent {
        owner.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression in givenCalls) {
                    val providerFunction = providerFunctionByGivenCall.getValue(expression)
                    DeclarationIrBuilder(pluginContext, expression.symbol).run {
                        val arguments = (expression.getValueArgument(0) as? IrVarargImpl)
                            ?.elements
                            ?.map { it as IrExpression } ?: emptyList()

                        val rawExpression = irCall(providerFunction).apply {
                            dispatchReceiver = provideContext(this@run, allScopes)
                        }

                        when {
                            arguments.isNotEmpty() -> DeclarationIrBuilder(
                                pluginContext,
                                expression.symbol
                            ).irCall(
                                rawExpression.type.classOrNull!!
                                    .owner
                                    .functions
                                    .first { it.name.asString() == "invoke" }
                            ).apply {
                                dispatchReceiver = rawExpression
                                arguments.forEachIndexed { index, argument ->
                                    putValueArgument(index, argument)
                                }
                            }
                            else -> rawExpression
                        }

                    }
                } else super.visitCall(expression)
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                if (expression !in readerCalls) return super.visitFunctionAccess(expression)
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

                if (result.isReaderLambdaInvoke()) {
                    return DeclarationIrBuilder(pluginContext, result.symbol).run {
                        IrCallImpl(
                            result.startOffset,
                            result.endOffset,
                            result.type,
                            if (result.symbol.owner.isSuspend) {
                                pluginContext.tmpSuspendFunction(result.symbol.owner.valueParameters.size + 1)
                                    .functions
                                    .first { it.owner.name.asString() == "invoke" }
                            } else {
                                pluginContext.tmpFunction(result.symbol.owner.valueParameters.size + 1)
                                    .functions
                                    .first { it.owner.name.asString() == "invoke" }
                            }
                        ).apply {
                            copyTypeAndValueArgumentsFrom(result)
                            putValueArgument(
                                valueArgumentsCount - 1,
                                provideContext(allScopes)
                            )
                        }
                    }
                }


                val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)
                val transformedCall = transformCall(transformedCallee, result)
                val contextArgument = getContextArgument(
                    owner, transformedCall,
                    genericFunctionMap
                ) { provideContext(this, allScopes) }

                return transformedCall.apply {
                    putValueArgument(valueArgumentsCount - 1, contextArgument)
                }
            }
        }, null)
    }

    private fun IrFunctionAccessExpression.getReaderLambdaArguments(): List<IrExpression> {
        return try {
            getArgumentsWithIr()
                .filter { (valueParameter, _) ->
                    valueParameter.type.hasAnnotation(InjektFqNames.Reader)
                }
                .map { it.second }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun IrExpression.getFunctionFromArgument() = when (this) {
        is IrFunctionExpression -> function
        else -> error("Cannot extract function from $this ${dumpSrc()}")
    }

    private fun getContextArgument(
        owner: IrDeclarationParent,
        call: IrFunctionAccessExpression,
        genericFunctionMap: List<Pair<IrFunction, IrFunction>>,
        provideContext: IrBuilderWithScope.() -> IrExpression
    ): IrExpression = DeclarationIrBuilder(pluginContext, call.symbol).run {
        val callee = call.symbol.owner
        if (!call.isReaderLambdaInvoke() && call.typeArgumentsCount != 0) {
            val calleeContext = transformFunctionIfNeeded(callee).getContext()!!

            irBlock(origin = IrStatementOrigin.OBJECT_LITERAL) {
                val contextImpl = buildClass {
                    name = Name.special("<context>")
                    visibility = Visibilities.LOCAL
                }.apply clazz@{
                    parent = owner
                    createImplicitParameterDeclarationWithWrappedDescriptor()

                    superTypes += calleeContext.defaultType
                        .typeWith(*call.typeArguments.toTypedArray())

                    call.getReaderLambdaArguments()
                        .forEach { expr ->
                            val transformedLambda =
                                transformFunctionIfNeeded(expr.getFunctionFromArgument())
                            superTypes += transformedLambda.getContext()!!.defaultType
                        }

                    addConstructor {
                        returnType = defaultType
                        isPrimary = true
                        visibility = Visibilities.PUBLIC
                    }.apply {
                        DeclarationIrBuilder(pluginContext, symbol).run {
                            body = irBlockBody {
                                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                                +IrInstanceInitializerCallImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    this@clazz.symbol,
                                    irBuiltIns.unitType
                                )
                            }
                        }
                    }

                    val implementedSuperTypes = mutableSetOf<IrType>()

                    fun implementFunctions(
                        superClass: IrClass,
                        typeArguments: List<IrType>
                    ) {
                        if (superClass.defaultType in implementedSuperTypes) return
                        implementedSuperTypes += superClass.defaultType
                        for (declaration in superClass.declarations.toList()) {
                            if (declaration !is IrFunction) continue
                            if (declaration is IrConstructor) continue
                            if (declaration.isFakeOverride) continue
                            if (declaration.dispatchReceiverParameter?.type == irBuiltIns.anyType) break
                            addFunction {
                                name = declaration.name
                                returnType = declaration.returnType
                                visibility = declaration.visibility
                            }.apply {
                                dispatchReceiverParameter =
                                    thisReceiver!!.copyTo(this)
                                addMetadataIfNotLocal()
                                body = DeclarationIrBuilder(
                                    pluginContext,
                                    symbol
                                ).irExprBody(
                                    irCall(
                                        genericFunctionMap.firstOrNull { (a, b) ->
                                            a == declaration &&
                                                    a.returnType.substitute(
                                                        superClass.typeParameters
                                                            .map { it.symbol }
                                                            .zip(typeArguments)
                                                            .toMap()
                                                    ) == b.returnType
                                        }?.second?.symbol ?: declaration.symbol
                                    ).apply {
                                        dispatchReceiver = provideContext()
                                    }
                                )
                            }
                        }

                        superClass.superTypes
                            .map { it to it.classOrNull?.owner }
                            .forEach { (superType, clazz) ->
                                if (clazz != null)
                                    implementFunctions(
                                        clazz,
                                        superType.typeArguments.map { it.typeOrFail })
                            }
                    }

                    superTypes.forEach { superType ->
                        implementFunctions(
                            superType.classOrNull!!.owner,
                            superType.typeArguments.map { it.typeOrFail })
                    }
                }
                +contextImpl
                +irCall(contextImpl.constructors.single())
            }
        } else {
            provideContext()
        }
    }

    private fun transformCall(
        transformedCallee: IrFunction,
        expression: IrFunctionAccessExpression
    ): IrFunctionAccessExpression {
        return when (expression) {
            is IrConstructorCall -> {
                IrConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    transformedCallee.returnType,
                    transformedCallee.symbol as IrConstructorSymbol,
                    expression.typeArgumentsCount,
                    transformedCallee.typeParameters.size,
                    transformedCallee.valueParameters.size,
                    expression.origin
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
            is IrDelegatingConstructorCall -> {
                IrDelegatingConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    transformedCallee.symbol as IrConstructorSymbol,
                    expression.typeArgumentsCount,
                    transformedCallee.valueParameters.size
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
            else -> {
                expression as IrCall
                IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    transformedCallee.returnType,
                    transformedCallee.symbol,
                    expression.origin,
                    expression.superQualifierSymbol
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
        }
    }

    private val IrFunction.isGiven: Boolean
        get() = descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given"

    private fun IrFunction.copyAsReader(): IrFunction {
        return copy(pluginContext).apply {
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .jvmNameAnnotation(name, pluginContext)
                correspondingPropertySymbol?.owner?.getter = this
            }

            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .jvmNameAnnotation(name, pluginContext)
                correspondingPropertySymbol?.owner?.setter = this
            }

            if (this@copyAsReader is IrOverridableDeclaration<*>) {
                overriddenSymbols = this@copyAsReader.overriddenSymbols.map {
                    val owner = it.owner as IrFunction
                    val newOwner = transformFunctionIfNeeded(owner)
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }
        }
    }

    private fun getExternalReaderContext(owner: IrDeclarationWithName): IrClass? {
        val declaration = if (owner is IrConstructor)
            owner.constructedClass else owner

        return pluginContext.moduleDescriptor.getPackage(declaration.getPackageFragment()!!.fqName)
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .map { pluginContext.referenceClass(it.fqNameSafe)!! }
            .map { it.owner }
            .filter { it.hasAnnotation(InjektFqNames.Name) }
            .singleOrNull { function ->
                function.getAnnotation(InjektFqNames.Name)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value == declaration.uniqueName()
            }
    }

    private fun IrElement.rewriteTransformedFunctionRefs() {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val result = super.visitFunctionExpression(expression) as IrFunctionExpression
                val transformed = transformFunctionIfNeeded(result.function)
                return if (transformed in transformedFunctions.values) IrFunctionExpressionImpl(
                    result.startOffset,
                    result.endOffset,
                    result.function.getFunctionType(pluginContext),
                    result.function,
                    result.origin
                )
                else result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val result = super.visitFunctionReference(expression) as IrFunctionReference
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) IrFunctionExpressionImpl(
                    result.startOffset,
                    result.endOffset,
                    result.symbol.owner.getFunctionType(pluginContext),
                    result.symbol.owner as IrSimpleFunction,
                    result.origin!!
                )
                else result
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall &&
                    result !is IrConstructorCall &&
                    result !is IrDelegatingConstructorCall
                ) return result
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) transformCall(
                    transformed,
                    result
                )
                else result
            }
        })
    }

}
