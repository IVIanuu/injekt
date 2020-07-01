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

import com.ivianuu.injekt.compiler.addToFileOrAbove
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

abstract class AbstractFunctionTransformer(
    pluginContext: IrPluginContext,
    private val transformOrder: TransformOrder
) : AbstractInjektTransformer(pluginContext) {

    protected val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    protected val originalTransformedFunctions = mutableSetOf<IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val functionsToTransform = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                return when (transformOrder) {
                    TransformOrder.BottomUp -> {
                        if (needsTransform(declaration)) functionsToTransform += declaration
                        super.visitFunction(declaration) as IrFunction
                    }
                    TransformOrder.TopDown -> {
                        val result = super.visitFunction(declaration) as IrFunction
                        if (needsTransform(result)) functionsToTransform += result
                        result
                    }
                }
            }
        })

        functionsToTransform.forEach { function ->
            val transformedFunction = transformFunctionIfNeeded(function)
            if (transformedFunction != function &&
                function.visibility != Visibilities.LOCAL
            ) {
                transformedFunction.addToFileOrAbove(function)
            }
        }

        val functionsToReplace = transformedFunctions
            .filter { it.key != it.value }
            .filter { it.key.visibility == Visibilities.LOCAL }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                return when (transformOrder) {
                    TransformOrder.BottomUp -> {
                        replaceFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
                    }
                    TransformOrder.TopDown -> {
                        super.visitFunction(replaceFunctionIfNeeded(declaration))
                    }
                }
            }

            private fun replaceFunctionIfNeeded(function: IrFunction): IrFunction =
                functionsToReplace[function] ?: function
        })

        declaration.rewriteTransformedFunctionRefs()

        return super.visitModuleFragment(declaration)
    }

    protected abstract fun needsTransform(function: IrFunction): Boolean

    protected abstract fun transform(
        function: IrFunction,
        callback: (IrFunction) -> Unit
    )

    protected abstract fun transformExternal(
        function: IrFunction,
        callback: (IrFunction) -> Unit
    )

    protected open fun transformFunctionExpression(
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

    protected open fun transformFunctionReference(
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

    protected open fun transformCall(
        transformedCallee: IrFunction,
        expression: IrCall
    ): IrCall {
        return IrCallImpl(
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

    protected fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!needsTransform(function)) return function

        originalTransformedFunctions += function

        val callback: (IrFunction) -> Unit = {
            transformedFunctions[function] = it
        }

        if (function.isExternalDeclaration())
            transformExternal(function, callback) else transform(function, callback)

        val transformedFunction = transformedFunctions.getValue(function)

        if (!function.isExternalDeclaration() && function != transformedFunction) {
            function.valueParameters
                .filter { it.defaultValue != null }
                .forEach {
                    it.defaultValue =
                        InjektDeclarationIrBuilder(pluginContext, function.symbol).run {
                            builder.irExprBody(irInjektIntrinsicUnit())
                        }
                }
            function.body = InjektDeclarationIrBuilder(pluginContext, function.symbol).run {
                builder.irExprBody(irInjektIntrinsicUnit())
            }
        }

        return transformedFunction
    }

    protected open fun isTransformedFunction(function: IrFunction): Boolean =
        function in transformedFunctions.values && function !in originalTransformedFunctions

    protected fun IrElement.rewriteTransformedFunctionRefs() {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val result = super.visitFunctionExpression(expression) as IrFunctionExpression
                val transformed = transformFunctionIfNeeded(result.function)
                return if (isTransformedFunction(transformed)) transformFunctionExpression(
                    transformed,
                    result
                )
                else result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val result = super.visitFunctionReference(expression) as IrFunctionReference
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (isTransformedFunction(transformed)) transformFunctionReference(
                    transformed,
                    result
                )
                else result
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (isTransformedFunction(transformed)) transformCall(
                    transformed,
                    result
                )
                else result
            }
        })
    }

    enum class TransformOrder {
        BottomUp, TopDown
    }

}
