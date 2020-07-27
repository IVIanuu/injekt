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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.canUseReader
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isMarkedAsReader
import com.ivianuu.injekt.compiler.jvmNameAnnotation
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderTransformer(
    pluginContext: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper
) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedClasses = mutableSetOf<IrClass>()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement =
                transformClassIfNeeded(super.visitClass(declaration) as IrClass)
        })

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement =
                transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
        })

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                return if (expression.symbol.owner.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.withReaderContext"
                ) {
                    DeclarationIrBuilder(pluginContext, expression.symbol).run {
                        irCall(
                            pluginContext.referenceFunctions(FqName("com.ivianuu.injekt.internalWithReaderContext"))
                                .single()
                        ).apply {
                            copyTypeAndValueArgumentsFrom(expression)
                        }
                    }
                } else result
            }
        })

        module.rewriteTransformedFunctionRefs()

        module.acceptVoid(symbolRemapper)

        val typeRemapper = ReaderTypeRemapper(pluginContext, symbolRemapper, symbols)
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
    }

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor(pluginContext.bindingContext)

        if (!clazz.isMarkedAsReader(pluginContext.bindingContext) && readerConstructor == null) return clazz

        if (readerConstructor == null) return clazz

        transformedClasses += clazz

        if (readerConstructor.valueParameters.lastOrNull()?.name?.asString() == "\$context")
            return clazz

        if (clazz.isExternalDeclaration()) {
            readerConstructor.addContextParameter()
            return clazz
        }

        val fieldsByValueParameters = mutableMapOf<IrValueParameter, IrField>()

        transformDeclaration(
            owner = clazz,
            ownerFunction = readerConstructor
        )

        readerConstructor.body = DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            irBlockBody {
                readerConstructor.body?.statements?.forEach {
                    +it
                    if (it is IrDelegatingConstructorCall) {
                        fieldsByValueParameters.forEach { (valueParameter, field) ->
                            +irSetField(
                                irGet(clazz.thisReceiver!!),
                                field,
                                irGet(valueParameter)
                            )
                        }
                    }
                }
            }
        }

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function is IrConstructor) {
            return if (function.canUseReader(pluginContext.bindingContext)) {
                transformClassIfNeeded(function.constructedClass)
                    .getReaderConstructor(pluginContext.bindingContext)!!
            } else function
        }

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.canUseReader(pluginContext.bindingContext)) return function

        if (function.valueParameters.lastOrNull()?.name?.asString() == "\$context")
            return function

        if (function.isExternalDeclaration()) {
            val transformedFunction = function.copyAsReader()
            transformedFunction.addContextParameter()
            transformedFunctions[function] = transformedFunction
            return transformedFunction
        }

        val transformedFunction = function.copyAsReader()
        transformedFunctions[function] = transformedFunction

        transformDeclaration(
            owner = transformedFunction,
            ownerFunction = transformedFunction
        )

        return transformedFunction
    }

    private fun <T> transformDeclaration(
        owner: T,
        ownerFunction: IrFunction
    ) where T : IrDeclarationWithName, T : IrDeclarationParent, T : IrTypeParametersContainer {
        ownerFunction.addContextParameter()

        owner.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()

            init {
                if (owner is IrFunction) functionStack += owner
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isMarkedAsReader(pluginContext.bindingContext)
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall &&
                    result !is IrConstructorCall &&
                    result !is IrDelegatingConstructorCall
                ) return result

                if (functionStack.isNotEmpty() &&
                    functionStack.lastOrNull() != owner
                ) return result

                val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)

                if (transformedCallee is IrSimpleFunction &&
                    transformedCallee.correspondingPropertySymbol?.descriptor?.fqNameSafe?.asString() == "com.ivianuu.injekt.readerContext"
                ) {
                    return DeclarationIrBuilder(pluginContext, ownerFunction.symbol)
                        .irGet(ownerFunction.valueParameters.last())
                }

                fun IrFunctionAccessExpression.addContextArgument() {
                    putValueArgument(
                        symbol.owner.valueParameters.size - 1,
                        DeclarationIrBuilder(pluginContext, ownerFunction.symbol)
                            .irGet(ownerFunction.valueParameters.last())
                    )
                }

                if (result.isReaderLambdaInvoke()) {
                    if (result.getArgumentsWithIr()
                            .map { it.second }
                            .any { it is IrGetValue && it.symbol.owner.name.asString() == "\$context" }
                    )
                        return result
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
                            addContextArgument()
                        }
                    }
                }

                if (!result.symbol.owner.canUseReader(pluginContext.bindingContext))
                    return result

                return when (result) {
                    is IrConstructorCall -> {
                        IrConstructorCallImpl(
                            result.startOffset,
                            result.endOffset,
                            transformedCallee.returnType,
                            transformedCallee.symbol as IrConstructorSymbol,
                            result.typeArgumentsCount,
                            transformedCallee.typeParameters.size,
                            transformedCallee.valueParameters.size,
                            result.origin
                        ).apply {
                            copyTypeAndValueArgumentsFrom(result)
                            addContextArgument()
                        }
                    }
                    is IrDelegatingConstructorCall -> {
                        IrDelegatingConstructorCallImpl(
                            result.startOffset,
                            result.endOffset,
                            result.type,
                            transformedCallee.symbol as IrConstructorSymbol,
                            result.typeArgumentsCount,
                            transformedCallee.valueParameters.size
                        ).apply {
                            copyTypeAndValueArgumentsFrom(result)
                            addContextArgument()
                        }
                    }
                    else -> {
                        result as IrCall
                        IrCallImpl(
                            result.startOffset,
                            result.endOffset,
                            transformedCallee.returnType,
                            transformedCallee.symbol,
                            result.origin,
                            result.superQualifierSymbol
                        ).apply {
                            copyTypeAndValueArgumentsFrom(result)
                            addContextArgument()
                        }
                    }
                }
            }
        })
    }

    private fun IrFunction.addContextParameter() {
        valueParameters += addValueParameter(
            name = "\$context",
            type = symbols.readerContext.defaultType
        )
    }

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

    private fun IrFunctionAccessExpression.isReaderLambdaInvoke(): Boolean {
        return symbol.owner.name.asString() == "invoke" &&
                dispatchReceiver?.type?.hasAnnotation(InjektFqNames.Reader) == true
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

    private fun transformFunctionExpression(
        transformedCallee: IrFunction,
        expression: IrFunctionExpression
    ): IrFunctionExpression {
        return IrFunctionExpressionImpl(
            expression.startOffset,
            expression.endOffset,
            transformedCallee.getFunctionType(pluginContext),
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
            transformedCallee.getFunctionType(pluginContext),
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

}
