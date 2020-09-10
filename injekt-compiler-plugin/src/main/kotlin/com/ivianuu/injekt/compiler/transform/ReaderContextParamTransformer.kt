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

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils

class ReaderContextParamTransformer(injektContext: InjektContext) : AbstractInjektTransformer(injektContext) {

    private val transformedClasses = mutableSetOf<IrClass>()
    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

    override fun lower() {
        injektContext.module.transformChildrenVoid(
            object : IrElementTransformerVoidWithContext() {
                override fun visitClassNew(declaration: IrClass): IrStatement =
                    super.visitClassNew(transformClassIfNeeded(declaration))

                override fun visitFunctionNew(declaration: IrFunction): IrStatement =
                    super.visitFunctionNew(transformFunctionIfNeeded(declaration))
            }
        )
        injektContext.module.rewriteTransformedReferences()

        injektContext.module.acceptVoid(injektContext.symbolRemapper)

        val typeRemapper = ReaderTypeRemapper(injektContext)
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            injektContext,
            injektContext.symbolRemapper,
            typeRemapper
        ).also { typeRemapper.deepCopy = it }
        injektContext.module.transformChildren(transformer, null)
        injektContext.module.patchDeclarationParents()
    }

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor(injektContext)

        if (!clazz.isReader(injektContext) && readerConstructor == null) return clazz

        if (readerConstructor == null) return clazz

        transformedClasses += clazz

        val contextParam = readerConstructor.addContextParameter()
        val contextField = clazz.addField(fieldName = "_context", fieldType = contextParam.type)

        readerConstructor.body = DeclarationIrBuilder(injektContext, clazz.symbol).run {
            irBlockBody {
                readerConstructor.body?.statements?.forEach {
                    +it
                    if (it is IrDelegatingConstructorCall) {
                        +irSetField(
                            irGet(clazz.thisReceiver!!),
                            contextField,
                            irGet(contextParam)
                        )
                    }
                }
            }
        }

        clazz.transformReaderCalls {
            DeclarationIrBuilder(injektContext, clazz.symbol).run {
                irGetField(
                    irGet(it.thisOfClass(clazz)!!),
                    contextField
                )
            }
        }

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function is IrConstructor) {
            return if (function.isReader(injektContext)) {
                transformClassIfNeeded(function.constructedClass)
                function
            } else function
        }

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.isReader(injektContext)) return function

        val transformedFunction = function.copyAsReader()
        transformedFunctions[function] = transformedFunction

        val contextParam = transformedFunction.addContextParameter()
        transformedFunction.transformReaderCalls {
            DeclarationIrBuilder(injektContext, transformedFunction.symbol)
                .irGet(contextParam)
        }

        return transformedFunction
    }

    private fun IrFunction.addContextParameter(): IrValueParameter {
        return addValueParameter(
            name = "_context",
            type = injektContext.injektSymbols.context.defaultType
        )
    }

    private fun IrDeclaration.transformReaderCalls(
        contextExpression: (List<ScopeWithIr>) -> IrExpression
    ) {
        transform(object : IrElementTransformerVoidWithContext() {
            var isNestedScope = false
            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                val wasNested = isNestedScope
                try {
                    isNestedScope = this@transformReaderCalls != declaration &&
                            declaration.isReader(injektContext)
                    return super.visitFunctionNew(declaration)
                } finally {
                    isNestedScope = wasNested
                }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val newExpression = if (!isNestedScope) {
                    transformCallIfNeeded(expression) {
                        contextExpression(allScopes)
                    }
                } else expression
                return super.visitFunctionAccess(newExpression)
            }
        }, null)
    }

    private fun transformCallIfNeeded(
        expression: IrFunctionAccessExpression,
        contextExpression: () -> IrExpression
    ): IrFunctionAccessExpression {
        return when (expression) {
            is IrConstructorCall -> {
                val callee = expression.symbol.owner
                if (!callee.isReader(injektContext)) return expression
                val transformedCallee = transformFunctionIfNeeded(callee)
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
                    putValueArgument(transformedCallee.valueParameters.lastIndex, contextExpression())
                }
            }
            is IrDelegatingConstructorCall -> {
                val callee = expression.symbol.owner
                if (!callee.isReader(injektContext)) return expression
                val transformedCallee = transformFunctionIfNeeded(callee)
                IrDelegatingConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    transformedCallee.symbol as IrConstructorSymbol,
                    transformedCallee.typeParameters.size,
                    transformedCallee.valueParameters.size
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                    putValueArgument(transformedCallee.valueParameters.lastIndex, contextExpression())
                }
            }
            else -> {
                expression as IrCall
                val callee = expression.symbol.owner
                if (!callee.isReader(injektContext) && !expression.isReaderLambdaInvoke()) return expression
                if (expression.isReaderLambdaInvoke()) {
                    val newCallee = when {
                        callee.dispatchReceiverParameter?.type?.isFunction() == true -> {
                            injektContext.tmpFunction(callee.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        callee.dispatchReceiverParameter?.type?.isKFunction() == true -> {
                            injektContext.tmpKFunction(callee.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        callee.dispatchReceiverParameter?.type?.isSuspendFunction() == true -> {
                            injektContext.tmpSuspendFunction(callee.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        callee.dispatchReceiverParameter?.type?.isKSuspendFunction() == true -> {
                            injektContext.tmpSuspendKFunction(callee.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        else -> error("Unexpected callee ${expression.dump()}")
                    }
                    IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        newCallee.returnType,
                        newCallee.symbol,
                        newCallee.typeParameters.size,
                        newCallee.valueParameters.size,
                        expression.origin,
                        expression.superQualifierSymbol
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                        putValueArgument(newCallee.valueParameters.lastIndex, contextExpression())
                    }
                } else {
                    val transformedCallee = transformFunctionIfNeeded(callee)
                    IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        transformedCallee.returnType,
                        transformedCallee.symbol,
                        transformedCallee.typeParameters.size,
                        transformedCallee.valueParameters.size,
                        expression.origin,
                        expression.superQualifierSymbol
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                        putValueArgument(transformedCallee.valueParameters.lastIndex, contextExpression())
                    }
                }
            }
        }
    }

    private fun IrFunction.copyAsReader(): IrFunction {
        return copy(
            injektContext
        ).apply {
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(injektContext, symbol)
                    .jvmNameAnnotation(name, injektContext)
                correspondingPropertySymbol?.owner?.getter = this
            }

            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(injektContext, symbol)
                    .jvmNameAnnotation(name, injektContext)
                correspondingPropertySymbol?.owner?.setter = this
            }

            if (this@copyAsReader is IrOverridableDeclaration<*>) {
                overriddenSymbols = this@copyAsReader.overriddenSymbols.map {
                    val owner = it.owner as IrFunction
                    val newOwner = when {
                        owner.dispatchReceiverParameter?.type?.isFunction() == true -> {
                            injektContext.tmpFunction(owner.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        owner.dispatchReceiverParameter?.type?.isKFunction() == true -> {
                            injektContext.tmpKFunction(owner.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        owner.dispatchReceiverParameter?.type?.isSuspendFunction() == true -> {
                            injektContext.tmpSuspendFunction(owner.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        owner.dispatchReceiverParameter?.type?.isKSuspendFunction() == true -> {
                            injektContext.tmpSuspendKFunction(owner.valueParameters.size + 1)
                                .owner
                                .functions
                                .first { it.name.asString() == "invoke" }
                        }
                        else -> transformFunctionIfNeeded(owner)
                    }
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }
        }
    }

    private fun IrModuleFragment.rewriteTransformedReferences() {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val result = super.visitFunctionExpression(expression) as IrFunctionExpression
                val transformed = transformFunctionIfNeeded(result.function)
                return if (transformed in transformedFunctions.values) IrFunctionExpressionImpl(
                    result.startOffset,
                    result.endOffset,
                    transformed.getFunctionType(injektContext),
                    transformed as IrSimpleFunction,
                    result.origin
                )
                else result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val result = super.visitFunctionReference(expression) as IrFunctionReference
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) IrFunctionReferenceImpl(
                    result.startOffset,
                    result.endOffset,
                    transformed.getFunctionType(injektContext),
                    transformed.symbol,
                    transformed.typeParameters.size,
                    transformed.valueParameters.size,
                    result.reflectionTarget,
                    result.origin
                )
                else result
            }
        })
    }

}
