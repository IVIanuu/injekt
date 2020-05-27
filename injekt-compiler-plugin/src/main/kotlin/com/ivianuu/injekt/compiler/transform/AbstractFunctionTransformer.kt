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

import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
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
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

abstract class AbstractFunctionTransformer(
    pluginContext: IrPluginContext,
    private val transformOrder: TransformOrder
) : AbstractInjektTransformer(pluginContext) {

    protected val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    protected val decoys = mutableMapOf<IrFunction, IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        declaration.rewriteTransformedFunctionCalls()
        return declaration
    }

    override fun visitFile(declaration: IrFile): IrFile {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitFile(declaration)
        result.patchWithDecoys(originalFunctions)
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitClass(declaration) as IrClass
        result.patchWithDecoys(originalFunctions)
        return result
    }

    private fun IrDeclarationContainer.patchWithDecoys(originalFunctions: List<IrFunction>) {
        for (original in originalFunctions) {
            val transformed = transformedFunctions[original]
            if (transformed != null && transformed != original) {
                declarations.add(createDecoy(original, transformed))
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return when (transformOrder) {
            TransformOrder.BottomUp -> transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
            TransformOrder.TopDown -> super.visitFunction(transformFunctionIfNeeded(declaration))
        }
    }

    protected abstract fun needsTransform(function: IrFunction): Boolean

    protected abstract fun transform(function: IrFunction): IrFunction

    protected abstract fun transformExternal(function: IrFunction): IrFunction

    protected abstract fun createDecoy(
        original: IrFunction,
        transformed: IrFunction
    ): IrFunction

    protected open fun transformFunctionExpression(
        transformed: IrFunction,
        expression: IrFunctionExpression
    ): IrFunctionExpression {
        return IrFunctionExpressionImpl(
            expression.startOffset,
            expression.endOffset,
            transformed.getFunctionType(irBuiltIns),
            transformed as IrSimpleFunction,
            expression.origin
        )
    }

    protected open fun transformFunctionReference(
        transformed: IrFunction,
        expression: IrFunctionReference
    ): IrFunctionReference {
        return IrFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            transformed.getFunctionType(irBuiltIns),
            transformed.symbol,
            expression.typeArgumentsCount,
            null,
            expression.origin
        )
    }

    protected open fun transformCall(
        transformed: IrFunction,
        expression: IrCall
    ): IrCall {
        return IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            transformed.returnType,
            transformed.symbol,
            expression.origin,
            expression.superQualifierSymbol
        ).apply {
            copyTypeAndValueArgumentsFrom(expression)
        }
    }

    protected fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        decoys[function]?.let { return it }

        if (!needsTransform(function)) return function

        val transformedFunction = if (function.isExternalDeclaration())
            transformExternal(function) else transform(function)

        transformedFunctions[function] = transformedFunction

        return transformedFunction
    }

    protected fun IrElement.rewriteTransformedFunctionCalls() {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val transformed = super.visitFunctionExpression(expression) as IrFunctionExpression
                return if (!needsTransform(transformed.function)) transformed
                else transformFunctionExpression(
                    transformFunctionIfNeeded(transformed.function), transformed
                )
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val transformed = super.visitFunctionReference(expression) as IrFunctionReference
                return if (!needsTransform(transformed.symbol.owner)) transformed
                else transformFunctionReference(
                    transformFunctionIfNeeded(transformed.symbol.owner), transformed
                )
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val transformed = super.visitCall(expression) as IrCall
                return if (!needsTransform(transformed.symbol.owner)) transformed
                else transformCall(
                    transformFunctionIfNeeded(transformed.symbol.owner), transformed
                )
            }
        })
    }

    enum class TransformOrder {
        BottomUp, TopDown
    }

}
