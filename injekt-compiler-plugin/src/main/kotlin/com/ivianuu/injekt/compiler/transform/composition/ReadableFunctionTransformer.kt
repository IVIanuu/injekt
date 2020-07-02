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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractFunctionTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReadableFunctionTransformer(
    pluginContext: IrPluginContext
) : AbstractFunctionTransformer(pluginContext) {

    fun getContextForFunction(readable: IrFunction): IrClass =
        transformFunctionIfNeeded(readable).valueParameters.last().type.classOrNull!!.owner

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)

        val symbolRemapper = DeepCopySymbolRemapper()
        declaration.acceptVoid(symbolRemapper)

        val typeRemapper = ReadableTypeRemapper(pluginContext, symbolRemapper)
        // for each declaration, we create a deepCopy transformer It is important here that we
        // use the "preserving metadata" variant since we are using this copy to *replace* the
        // originals, or else the module we would produce wouldn't have any metadata in it.
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            pluginContext,
            symbolRemapper,
            typeRemapper
        ).also { typeRemapper.deepCopy = it }
        declaration.files.forEach {
            it.transformChildren(
                transformer,
                null
            )
        }
        // just go through and patch all of the parents to make sure things are properly wired
        // up.
        declaration.patchDeclarationParents()

        transformedFunctions
            .filterNot { it.key.isExternalDeclaration() }
            .forEach {
                val contextClass = it.value.valueParameters.last().type.classOrNull!!.owner
                if (contextClass !in it.value.file.declarations) {
                    it.value.file.addChild(contextClass)
                }
            }

        return declaration
    }

    override fun needsTransform(function: IrFunction): Boolean =
        function.isReadable(pluginContext.bindingContext)

    override fun transform(function: IrFunction, callback: (IrFunction) -> Unit) {
        val transformedFunction = function.copy().apply {
            callback(this)
            overriddenSymbols += (function as IrFunctionImpl).overriddenSymbols.map {
                transformFunctionIfNeeded(it.owner).symbol as IrSimpleFunctionSymbol
            }
        }

        if (function.valueParameters.any { it.name.asString() == "readable_context" }) {
            return
        }

        val parametersMap = transformedFunction.valueParameters.associateWith { valueParameter ->
            val defaultExpr = valueParameter.defaultValue?.expression
            val isGiven = defaultExpr is IrCall && defaultExpr.symbol.descriptor
                .fqNameSafe.asString() == "com.ivianuu.injekt.composition.given"
            valueParameter.copyTo(
                transformedFunction,
                index = valueParameter.index,
                type = if (isGiven) irBuiltIns.anyNType else valueParameter.type
            )
        }
        transformedFunction.valueParameters = parametersMap.values.toList()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return parametersMap[expression.symbol.owner]
                    ?.let { DeclarationIrBuilder(pluginContext, it.symbol).irGet(it) }
                    ?: super.visitGetValue(expression)
            }
        })

        val thisParameters = transformedFunction.valueParameters
            .filter { valueParameter ->
                val defaultExpr = valueParameter.defaultValue?.expression
                defaultExpr is IrCall && defaultExpr.symbol.descriptor
                    .fqNameSafe.asString() == "com.ivianuu.injekt.composition.given"
            }
        thisParameters
            .forEach {
                it.defaultValue = DeclarationIrBuilder(pluginContext, it.symbol).run {
                    irExprBody(irGetObject(symbols.uninitialized))
                }
            }

        val contextClass = buildClass {
            kind = ClassKind.INTERFACE
            name = InjektNameConventions.getContextClassNameForReadableFunction(
                function.getPackageFragment()!!.fqName,
                function
            )
        }.apply {
            parent = function.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(transformedFunction)
            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.astName.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(transformedFunction.descriptor.fqNameSafe.asString())
                    )
                }
            }
        }

        val originalTypeByValueParameter = thisParameters.associateWith {
            function.allParameters[it.index].type
                .remapTypeParameters(function, transformedFunction)
        }

        val providerFunctionByParameter = thisParameters.associateWith { valueParameter ->
            contextClass.addFunction {
                name = InjektNameConventions.getReadableContextParamNameForValueParameter(
                    function.file, valueParameter
                )
                returnType = originalTypeByValueParameter[valueParameter]!!
                    .remapTypeParameters(transformedFunction, contextClass)
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
            }
        }.mapKeys { it.key.symbol }

        val readableCalls = mutableListOf<IrCall>()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>(transformedFunction)

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReadable = declaration.isReadable(pluginContext.bindingContext)
                if (isReadable) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReadable) functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (functionStack.last() == transformedFunction &&
                    (expression.symbol.owner.isReadable(pluginContext.bindingContext) ||
                            expression.isReadableLambdaInvoke())
                ) {
                    readableCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        val genericFunctionMap = mutableListOf<Pair<IrFunction, IrFunction>>()

        fun addFunctionsFromGenericContext(
            genericContext: IrClass,
            element: IrElement,
            typeArguments: List<IrType>
        ) {
            contextClass.superTypes += genericContext.superTypes
            genericContext.functions
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .filterNot { it is IrConstructor }
                .forEach { genericContextFunction ->
                    genericFunctionMap += genericContextFunction to contextClass.addFunction {
                        name = InjektNameConventions
                            .getReadableContextParamNameForContext(
                                genericContextFunction.getPackageFragment()!!.fqName,
                                element,
                                genericContextFunction
                            )
                        returnType = genericContextFunction.returnType
                            .substituteAndKeepQualifiers(genericContext.typeParameters
                                .map { it.symbol }
                                .zip(typeArguments).toMap())
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
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
            element: IrElement,
            typeArguments: List<IrType>
        ) {
            if (subcontext.typeParameters.isNotEmpty()) {
                addFunctionsFromGenericContext(
                    subcontext, element, typeArguments
                )
            } else {
                addSubcontext(subcontext, typeArguments)
            }
        }

        readableCalls
            .forEach { call ->
                if (!call.isReadableLambdaInvoke()) {
                    val callContext = getContextForFunction(call.symbol.owner)
                    handleSubcontext(callContext, call, call.typeArguments)
                }
                call.getReadableLambdas()
                    .forEach { expr ->
                        val transformedLambda = transformFunctionIfNeeded(expr.function)
                        val lambdaContext = getContextForFunction(transformedLambda)
                        handleSubcontext(lambdaContext, expr, emptyList())
                    }
            }

        val contextValueParameter = transformedFunction.addValueParameter(
            "readable_context",
            contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
        )

        val variableByValueParameter = mutableMapOf<IrValueSymbol, IrVariable>()
        val ignoredGetValues = mutableSetOf<IrGetValue>()

        if (function.body != null) {
            transformedFunction.body =
                with(DeclarationIrBuilder(pluginContext, transformedFunction.symbol)) {
                    irBlockBody {
                        thisParameters.forEach { valueParameter ->
                            variableByValueParameter[valueParameter.symbol] = createTmpVariable(
                                irIfThenElse(
                                    valueParameter.type,
                                    irEqeqeq(
                                        irGet(valueParameter)
                                            .also { ignoredGetValues += it },
                                        irGetObject(symbols.uninitialized)
                                    ),
                                    irCall(providerFunctionByParameter.getValue(valueParameter.symbol)).apply {
                                        dispatchReceiver = irGet(contextValueParameter)
                                    },
                                    irImplicitCast(
                                        irGet(valueParameter)
                                            .also { ignoredGetValues += it },
                                        originalTypeByValueParameter[valueParameter]!!
                                    )
                                )
                            )
                        }

                        transformedFunction.body?.statements?.forEach { +it }
                    }
                }
        }

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return if (expression in ignoredGetValues) super.visitGetValue(expression)
                else variableByValueParameter[expression.symbol]
                    ?.let { DeclarationIrBuilder(pluginContext, it.symbol).irGet(it) }
                    ?: super.visitGetValue(expression)
            }
        })

        rewriteReadableCalls(transformedFunction, genericFunctionMap)
    }

    private fun IrCall.getReadableLambdas(): List<IrFunctionExpression> {
        return getArgumentsWithIr()
            .filter { (valueParameter, expr) ->
                (valueParameter.type.isFunction() || valueParameter.type.isSuspendFunction()) &&
                        valueParameter.type.hasAnnotation(InjektFqNames.Readable) &&
                        expr is IrFunctionExpression
            }
            .map { it.second as IrFunctionExpression }
    }

    private fun rewriteReadableCalls(
        function: IrFunction,
        genericFunctionMap: List<Pair<IrFunction, IrFunction>>
    ) {
        function.rewriteTransformedFunctionRefs()

        function.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = mutableListOf(function)

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReadable = declaration.isReadable(pluginContext.bindingContext)
                if (isReadable) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReadable) functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (functionStack.last() != function) return result
                if (!result.symbol.owner.isReadable(pluginContext.bindingContext) &&
                    !result.isReadableLambdaInvoke()
                ) return result

                if (result.isReadableLambdaInvoke()) {
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
                                irGet(function.valueParameters.last())
                            )
                        }
                    }
                }

                val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)
                val transformedCall = transformCall(transformedCallee, result)

                val contextArgument =
                    DeclarationIrBuilder(pluginContext, transformedCall.symbol).run {
                        if (transformedCallee.typeParameters.isNotEmpty() && !transformedCall.isReadableLambdaInvoke()) {
                            val calleeContext = getContextForFunction(transformedCallee)

                            irBlock(origin = IrStatementOrigin.OBJECT_LITERAL) {
                                val contextImpl = buildClass {
                                    name = Name.special("<context>")
                                    visibility = Visibilities.LOCAL
                                }.apply clazz@{
                                    parent = function
                                    createImplicitParameterDeclarationWithWrappedDescriptor()

                                    superTypes += calleeContext.defaultType
                                        .typeWith(*transformedCall.typeArguments.toTypedArray())

                                    result.getReadableLambdas()
                                        .forEach { expr ->
                                            val transformedLambda =
                                                transformFunctionIfNeeded(expr.function)
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
                                                // todo this would be correct but codegen fails
                                                /*.substituteAndKeepQualifiers(
                                                    superClass.typeParameters.map { it.symbol }.associateWith {
                                                        typeArguments[it.owner.index]
                                                    }
                                                )*/
                                                visibility = declaration.visibility
                                            }.apply {
                                                dispatchReceiverParameter =
                                                    thisReceiver!!.copyTo(this)
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
                                                        dispatchReceiver =
                                                            irGet(function.valueParameters.last())
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
                            irGet(function.valueParameters.last())
                        }
                    }

                return transformedCall.apply {
                    putValueArgument(valueArgumentsCount - 1, contextArgument)
                }
            }
        })
    }

    override fun transformExternal(function: IrFunction, callback: (IrFunction) -> Unit) {
        val transformedFunction = function.copy()
        callback(transformedFunction)

        if (transformedFunction.valueParameters.any { it.name.asString() == "readable_context" }) {
            return
        }

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
            "readable_context",
            contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
        )
    }

}

