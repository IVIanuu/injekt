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

package com.ivianuu.injekt.compiler.transform.reader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.getSuspendFunctionType
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
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
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderTransformer(
    pluginContext: IrPluginContext,
    val symbolRemapper: DeepCopySymbolRemapper
) : AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedClasses = mutableSetOf<IrClass>()

    private val globalNameProvider = NameProvider()

    fun getContextForFunction(reader: IrFunction): IrClass =
        transformFunctionIfNeeded(reader).valueParameters.last().type.classOrNull!!.owner

    fun getTransformedFunction(function: IrFunction): IrFunction =
        transformFunctionIfNeeded(function)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement =
                transformClassIfNeeded(super.visitClass(declaration) as IrClass)

            override fun visitFunction(declaration: IrFunction): IrStatement =
                transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
        })

        addContextClassesToFiles()

        // todo check if this is needed
        declaration.rewriteTransformedFunctionRefs()

        declaration.acceptVoid(symbolRemapper)

        val typeRemapper = ReaderTypeRemapper(pluginContext, symbolRemapper)
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            pluginContext,
            symbolRemapper,
            typeRemapper
        )
        declaration.files.forEach {
            it.transformChildren(
                transformer,
                null
            )
        }
        declaration.patchDeclarationParents()

        return super.visitModuleFragment(declaration)
    }

    fun addContextClassesToFiles() {
        transformedFunctions
            .filterNot { it.key.isExternalDeclaration() }
            .forEach {
                val contextClass = it.value.valueParameters.last().type.classOrNull!!.owner
                if (contextClass !in it.value.file.declarations) {
                    it.value.file.addChild(contextClass)
                }
            }

        transformedClasses
            .filterNot { it.isExternalDeclaration() }
            .forEach {
                val contextClass =
                    it.getReaderConstructor()!!.valueParameters.last().type.classOrNull!!.owner
                if (contextClass !in it.file.declarations) {
                    it.file.addChild(contextClass)
                }
            }
    }

    private fun IrFunctionAccessExpression.getReaderLambdaArguments(): List<IrExpression> {
        return try {
            getArgumentsWithIr()
                .filter { (valueParameter, _) ->
                    (valueParameter.type.isFunction() || valueParameter.type.isSuspendFunction()) &&
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

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor()

        if (!clazz.hasAnnotation(InjektFqNames.Reader) &&
            readerConstructor == null
        ) return clazz

        if (readerConstructor == null) return clazz

        if (readerConstructor.valueParameters.any { it.name.asString() == "_context" })
            return clazz

        transformedClasses += clazz

        if (clazz.isExternalDeclaration()) {
            TODO()
            return clazz
        }

        val parentFunction = if (clazz.visibility == Visibilities.LOCAL &&
            clazz.parent is IrFunction
        ) {
            clazz.parent as IrFunction
        } else null

        val contextClassFunctionNameProvider = NameProvider()

        val contextClass = buildClass {
            kind = ClassKind.INTERFACE
            name = globalNameProvider.getContextClassName(clazz)
        }.apply {
            parent = clazz.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(clazz)
            parentFunction?.let { copyTypeParametersFrom(it) }

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.astName.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(clazz.descriptor.fqNameSafe.asString())
                    )
                }
            }
        }

        val getCalls = mutableListOf<IrCall>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        clazz.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (functionStack.isNotEmpty()) return result
                if (result !is IrCall && result !is IrConstructorCall) return result
                if (expression.symbol.owner.isReader(pluginContext.bindingContext) ||
                    expression.isReaderLambdaInvoke() ||
                    expression.isReaderConstructorCall()
                ) {
                    if (result is IrCall &&
                        result.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get"
                    ) {
                        getCalls += result
                    } else {
                        readerCalls += result
                    }
                }
                return result
            }
        })

        val providerFunctionByGetCall = getCalls.associateWith { getCall ->
            contextClass.addFunction {
                name = contextClassFunctionNameProvider.getProvideFunctionNameForGetCall(
                    clazz,
                    getCall.type
                        .remapTypeParameters(clazz, contextClass)
                        .let {
                            if (parentFunction != null) {
                                it.remapTypeParameters(parentFunction, contextClass)
                            } else it
                        }
                )
                returnType = getCall.type
                    .remapTypeParameters(clazz, contextClass)
                    .let {
                        if (parentFunction != null) {
                            it.remapTypeParameters(parentFunction, contextClass)
                        } else it
                    }
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
                addMetadataIfNotLocal()
            }
        }

        val genericFunctionMap = mutableListOf<Pair<IrFunction, IrFunction>>()

        fun addFunctionsFromGenericContext(
            genericContext: IrClass,
            typeArguments: List<IrType>
        ) {
            println("${clazz.render()} add functions from generic context ${genericContext.dumpSrc()}")
            contextClass.superTypes += genericContext.superTypes
            genericContext.functions
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .filterNot { it is IrConstructor }
                .forEach { genericContextFunction ->
                    genericFunctionMap += genericContextFunction to contextClass.addFunction {
                        name = contextClassFunctionNameProvider.getProvideFunctionNameForGetCall(
                            clazz,
                            genericContextFunction.returnType
                        )
                        returnType = genericContextFunction.returnType
                            .substituteAndKeepQualifiers(genericContext.typeParameters
                                .map { it.symbol }
                                .zip(typeArguments).toMap()
                            )
                            .remapTypeParameters(clazz, contextClass)
                            .let {
                                if (parentFunction != null) {
                                    it.remapTypeParameters(parentFunction, contextClass)
                                } else it
                            }
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()
                    }
                }
        }

        fun addSubcontext(
            subcontext: IrClass,
            typeArguments: List<IrType>
        ) {
            contextClass.superTypes += subcontext.defaultType.typeWith(*typeArguments.toTypedArray())
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

        readerCalls
            .forEach { call ->
                if (!call.isReaderLambdaInvoke()) {
                    val callContext = getContextForFunction(call.symbol.owner)
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
                        val lambdaContext = getContextForFunction(transformedLambda)
                        handleSubcontext(lambdaContext, emptyList())
                    }
            }

        val contextField = clazz.addField(
            "_context".asNameId(),
            contextClass.typeWith(clazz.typeParameters.map { it.defaultType }),
            Visibilities.PRIVATE
        )

        readerConstructor.body = DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            irBlockBody {
                val contextValueParameter = readerConstructor.addValueParameter(
                    "_context",
                    contextClass.typeWith(clazz.typeParameters.map { it.defaultType })
                )
                readerConstructor.body?.statements?.forEach {
                    +it.deepCopyWithSymbols()
                }
                +irSetField(
                    irGet(clazz.thisReceiver!!),
                    contextField,
                    irGet(contextValueParameter)
                )
            }
        }

        clazz.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression in getCalls) {
                    val providerFunction = providerFunctionByGetCall.getValue(expression)
                    DeclarationIrBuilder(pluginContext, expression.symbol).run {
                        irCall(providerFunction).apply {
                            dispatchReceiver = irGetField(
                                irGet(allScopes.thisOfClass(clazz)!!),
                                contextField
                            )
                        }
                    }
                } else super.visitCall(expression)
            }
        }, null)

        rewriteReaderCalls(clazz, genericFunctionMap) {
            irGet(it.thisOfClass(clazz)!!)
        }

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function is IrConstructor) return function

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.isReader(pluginContext.bindingContext)) return function

        if (function.isExternalDeclaration()) {
            val transformedFunction = function.copy(pluginContext)
            transformedFunctions[function] = transformedFunction

            if (transformedFunction.valueParameters.any { it.name.asString() == "_context" }) {
                return transformedFunction
            }

            try {
                val contextClass =
                    moduleFragment.descriptor.getPackage(function.getPackageFragment()!!.fqName)
                        .memberScope
                        .getContributedDescriptors()
                        .filterIsInstance<ClassDescriptor>()
                        .filter { it.hasAnnotation(InjektFqNames.AstName) }
                        .single {
                            it.annotations.findAnnotation(InjektFqNames.AstName)!!
                                .argumentValue("name")
                                .let { it as StringValue }
                                .value == function.descriptor.fqNameSafe.asString()
                        }
                        .let { pluginContext.referenceClass(it.fqNameSafe)!!.owner }

                transformedFunction.addValueParameter(
                    "_context",
                    contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
                )
            } catch (e: Exception) {
                error("Couldn't find context fo r ${transformedFunction.dump()}")
            }

            return transformedFunction
        }

        val transformedFunction = function.copy(pluginContext).apply {
            transformedFunctions[function] = this

            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol).jvmNameAnnotation(
                    name
                )
                correspondingPropertySymbol?.owner?.getter = this
            }

            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol).jvmNameAnnotation(
                    name
                )
                correspondingPropertySymbol?.owner?.setter = this
            }
        }

        if (transformedFunction.valueParameters.any { it.name.asString() == "_context" }) {
            return transformedFunction
        }

        val contextClassFunctionNameProvider = NameProvider()

        val parentFunction = if (transformedFunction.visibility == Visibilities.LOCAL &&
            transformedFunction.parent is IrFunction
        ) {
            transformedFunction.parent as IrFunction
        } else null

        val contextClass = buildClass {
            kind = ClassKind.INTERFACE
            name = globalNameProvider.getContextClassName(function)
        }.apply {
            parent = function.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(transformedFunction)
            parentFunction?.let { copyTypeParametersFrom(it) }

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.astName.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(transformedFunction.descriptor.fqNameSafe.asString())
                    )
                }
            }
        }

        val getCalls = mutableListOf<IrCall>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        transformedFunction.transform(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall && result !is IrConstructorCall) return result
                if (functionStack.lastOrNull() != transformedFunction) return result
                if (expression.symbol.owner.isReader(pluginContext.bindingContext) ||
                    expression.isReaderLambdaInvoke() ||
                    expression.isReaderConstructorCall()
                ) {
                    if (result is IrCall &&
                        expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get"
                    ) {
                        getCalls += result
                    } else {
                        readerCalls += result
                    }
                }
                return result
            }
        }, null)

        val providerFunctionByGetCall = getCalls.associateWith { getCall ->
            contextClass.addFunction {
                name = contextClassFunctionNameProvider.getProvideFunctionNameForGetCall(
                    transformedFunction,
                    getCall.type
                        .remapTypeParameters(transformedFunction, contextClass)
                        .let {
                            if (parentFunction != null) {
                                it.remapTypeParameters(parentFunction, contextClass)
                            } else it
                        }
                )
                returnType = getCall.type
                    .remapTypeParameters(transformedFunction, contextClass)
                    .let {
                        if (parentFunction != null) {
                            it.remapTypeParameters(parentFunction, contextClass)
                        } else it
                    }
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
                addMetadataIfNotLocal()
            }
        }

        val genericFunctionMap = mutableListOf<Pair<IrFunction, IrFunction>>()

        fun addFunctionsFromGenericContext(
            genericContext: IrClass,
            typeArguments: List<IrType>
        ) {
            println("${transformedFunction.render()} add functions from generic context ${genericContext.dumpSrc()}")
            contextClass.superTypes += genericContext.superTypes
            genericContext.functions
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .filterNot { it is IrConstructor }
                .forEach { genericContextFunction ->
                    genericFunctionMap += genericContextFunction to contextClass.addFunction {
                        name = contextClassFunctionNameProvider.getProvideFunctionNameForGetCall(
                            transformedFunction,
                            genericContextFunction.returnType
                        )
                        returnType = genericContextFunction.returnType
                            .substituteAndKeepQualifiers(genericContext.typeParameters
                                .map { it.symbol }
                                .zip(typeArguments).toMap()
                            )
                            .remapTypeParameters(transformedFunction, contextClass)
                            .let {
                                if (parentFunction != null) {
                                    it.remapTypeParameters(parentFunction, contextClass)
                                } else it
                            }
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()
                    }
                }
        }

        fun addSubcontext(
            subcontext: IrClass,
            typeArguments: List<IrType>
        ) {
            contextClass.superTypes += subcontext.defaultType.typeWith(*typeArguments.toTypedArray())
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

        readerCalls
            .forEach { call ->
                if (!call.isReaderLambdaInvoke()) {
                    val callContext = getContextForFunction(call.symbol.owner)
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
                        val lambdaContext = getContextForFunction(transformedLambda)
                        handleSubcontext(lambdaContext, emptyList())
                    }
            }

        val contextValueParameter = transformedFunction.addValueParameter(
            "_context",
            contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
        )

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression in getCalls) {
                    val providerFunction = providerFunctionByGetCall.getValue(expression)
                    DeclarationIrBuilder(pluginContext, expression.symbol).run {
                        irCall(providerFunction).apply {
                            dispatchReceiver = irGet(contextValueParameter)
                        }
                    }
                } else super.visitCall(expression)
            }
        })

        rewriteReaderCalls(transformedFunction, genericFunctionMap) {
            irGet(transformedFunction.valueParameters.last())
        }

        return transformedFunction
    }

    private fun <T> rewriteReaderCalls(
        owner: T,
        genericFunctionMap: List<Pair<IrFunction, IrFunction>>,
        getContext: IrBuilderWithScope.(List<ScopeWithIr>) -> IrExpression
    ) where T : IrDeclaration, T : IrDeclarationParent {
        owner.rewriteTransformedFunctionRefs()

        owner.transform(object : IrElementTransformerVoidWithContext() {
            private val functionStack = mutableListOf<IrFunction>()

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunctionNew(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall && result !is IrConstructorCall)
                    return result
                if (functionStack.lastOrNull() != owner &&
                    functionStack.lastOrNull() != null
                ) return result
                if (!result.symbol.owner.isReader(pluginContext.bindingContext) &&
                    !result.isReaderLambdaInvoke() &&
                    !result.isReaderConstructorCall()
                ) return result

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
                                getContext(allScopes)
                            )
                        }
                    }
                }

                val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)
                val transformedCall = if (transformedCallee != transformedFunctions.values)
                    transformCall(transformedCallee, result) else result

                val contextArgument =
                    DeclarationIrBuilder(pluginContext, transformedCall.symbol).run {
                        if (transformedCallee.typeParameters.isNotEmpty() && !transformedCall.isReaderLambdaInvoke()) {
                            val calleeContext = getContextForFunction(transformedCallee)

                            irBlock(origin = IrStatementOrigin.OBJECT_LITERAL) {
                                val contextImpl = buildClass {
                                    name = Name.special("<context>")
                                    visibility = Visibilities.LOCAL
                                }.apply clazz@{
                                    parent = owner
                                    createImplicitParameterDeclarationWithWrappedDescriptor()

                                    superTypes += calleeContext.defaultType
                                        .typeWith(*transformedCall.typeArguments.toTypedArray())

                                    result.getReaderLambdaArguments()
                                        .forEach { expr ->
                                            val transformedLambda =
                                                transformFunctionIfNeeded(expr.getFunctionFromArgument())
                                            superTypes += getContextForFunction(transformedLambda)
                                                .defaultType
                                        }

                                    addConstructor {
                                        returnType = defaultType
                                        isPrimary = true
                                        visibility = Visibilities.PUBLIC
                                    }.apply {
                                        InjektDeclarationIrBuilder(pluginContext, symbol).run {
                                            body = builder.irBlockBody {
                                                initializeClassWithAnySuperClass(this@clazz.symbol)
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
                                                    .substituteAndKeepQualifiers(
                                                        superClass.typeParameters.map { it.symbol }
                                                            .associateWith {
                                                                typeArguments[it.owner.index]
                                                            }
                                                    )
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
                                                                    a.returnType.substituteAndKeepQualifiers(
                                                                        superClass.typeParameters
                                                                            .map { it.symbol }
                                                                            .zip(typeArguments)
                                                                            .toMap()
                                                                    ) == b.returnType
                                                        }?.second?.symbol ?: declaration.symbol
                                                    ).apply {
                                                        dispatchReceiver = getContext(allScopes)
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
                                            superType.getClass()!!,
                                            superType.typeArguments.map { it.typeOrFail })
                                    }
                                }
                                +contextImpl
                                +irCall(contextImpl.constructors.single())
                            }
                        } else {
                            getContext(allScopes)
                        }
                    }

                return transformedCall.apply {
                    putValueArgument(valueArgumentsCount - 1, contextArgument)
                }
            }
        }, null)
    }

    private fun NameProvider.getContextClassName(declaration: IrDeclarationWithName): Name {
        return getBaseName(declaration, "Context")
    }

    private fun NameProvider.getBaseName(
        declaration: IrDeclarationWithName,
        suffix: String
    ): Name = allocateForGroup(
        getJoinedName(
            declaration.getPackageFragment()!!.fqName,
            declaration.descriptor.fqNameSafe
                .parent()
                .let {
                    if (declaration.name.isSpecial) {
                        it.child(allocateForGroup("Lambda"))
                    } else {
                        it.child(declaration.name.asString())
                    }
                }
        ).asString() + "_$suffix"
    ).asNameId()

    private fun NameProvider.getProvideFunctionNameForGetCall(
        declaration: IrDeclarationWithName,
        type: IrType
    ): Name = getBaseName(declaration, type.classifierOrFail.descriptor.name.asString())

    private fun transformFunctionExpression(
        transformedCallee: IrFunction,
        expression: IrFunctionExpression
    ): IrFunctionExpression {
        return IrFunctionExpressionImpl(
            expression.startOffset,
            expression.endOffset,
            if (transformedCallee.isSuspend) transformedCallee.getSuspendFunctionType(pluginContext)
            else transformedCallee.getFunctionType(pluginContext),
            transformedCallee as IrSimpleFunction,
            expression.origin
        )
    }

    private fun transformFunctionReference(
        transformedCallee: IrFunction,
        expression: IrFunctionReference
    ): IrFunctionReference {
        return IrFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            if (transformedCallee.isSuspend) transformedCallee.getSuspendFunctionType(pluginContext)
            else transformedCallee.getFunctionType(pluginContext),
            transformedCallee.symbol,
            expression.typeArgumentsCount,
            null,
            expression.origin
        )
    }

    private fun transformCall(
        transformedCallee: IrFunction,
        expression: IrFunctionAccessExpression
    ): IrFunctionAccessExpression {
        return if (expression is IrConstructorCall) {
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
                try {
                    copyTypeAndValueArgumentsFrom(expression)
                } catch (e: Throwable) {
                    error("Couldn't transform ${expression.dumpSrc()} to ${transformedCallee.render()}")
                }
            }
        } else {
            expression as IrCall
            IrCallImpl(
                expression.startOffset,
                expression.endOffset,
                transformedCallee.returnType,
                transformedCallee.symbol,
                expression.origin,
                expression.superQualifierSymbol
            ).apply {
                try {
                    copyTypeAndValueArgumentsFrom(expression)
                } catch (e: Throwable) {
                    error("Couldn't transform ${expression.dumpSrc()} to ${transformedCallee.render()}")
                }
            }
        }
    }

    private fun IrElement.rewriteTransformedFunctionRefs() {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val result = super.visitFunctionExpression(expression) as IrFunctionExpression
                val transformed = transformFunctionIfNeeded(result.function)
                return if (transformed in transformedFunctions.values) transformFunctionExpression(
                    transformed,
                    result
                )
                else result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val result = super.visitFunctionReference(expression) as IrFunctionReference
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) transformFunctionReference(
                    transformed,
                    result
                )
                else result
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall &&
                    result !is IrConstructorCall
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
