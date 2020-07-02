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
import com.ivianuu.injekt.compiler.getSuspendFunctionType
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyBodyTo
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource

abstract class AbstractFunctionTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    protected val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    protected val originalTransformedFunctions = mutableSetOf<IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement =
                transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
        })

        // todo check if this is needed
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
            if (transformedCallee.isSuspend) transformedCallee.getSuspendFunctionType(pluginContext)
            else transformedCallee.getFunctionType(pluginContext),
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
            if (transformedCallee.isSuspend) transformedCallee.getSuspendFunctionType(pluginContext)
            else transformedCallee.getFunctionType(pluginContext),
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

        if (function.isExternalDeclaration()) transformExternal(function, callback)
        else transform(function, callback)

        return transformedFunctions.getValue(function)
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

    private fun wrapDescriptor(descriptor: FunctionDescriptor): WrappedSimpleFunctionDescriptor {
        return when (descriptor) {
            is PropertyGetterDescriptor ->
                WrappedPropertyGetterDescriptor(
                    descriptor.annotations,
                    descriptor.source
                )
            is PropertySetterDescriptor ->
                WrappedPropertySetterDescriptor(
                    descriptor.annotations,
                    descriptor.source
                )
            is DescriptorWithContainerSource ->
                WrappedFunctionDescriptorWithContainerSource(descriptor.containerSource)
            else ->
                WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)
        }
    }

    fun IrFunction.copy(
        isInline: Boolean = this.isInline,
        modality: Modality = descriptor.modality
    ): IrSimpleFunction {
        val descriptor = descriptor
        val newDescriptor = wrapDescriptor(descriptor)

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            IrSimpleFunctionSymbolImpl(newDescriptor),
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            descriptor.isTailrec,
            descriptor.isSuspend,
            descriptor.isOperator,
            isExpect,
            isFakeOverride
        ).also { fn ->
            newDescriptor.bind(fn)
            if (this is IrSimpleFunction) {
                fn.correspondingPropertySymbol = correspondingPropertySymbol
            }
            fn.parent = parent
            fn.copyTypeParametersFrom(this)
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            fn.valueParameters = valueParameters.map { p ->
                p.copyTo(fn, name = dexSafeName(p.name))
            }
            fn.annotations = annotations.map { a -> a }
            fn.metadata = metadata
            fn.body = copyBodyTo(fn)
            fn.allParameters.forEach { valueParameter ->
                valueParameter.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return allParameters
                            .mapIndexed { index, valueParameter -> valueParameter to index }
                            .singleOrNull { it.first.symbol == expression.symbol }
                            ?.let { fn.allParameters[it.second] }
                            ?.let { DeclarationIrBuilder(pluginContext, fn.symbol).irGet(it) }
                            ?: super.visitGetValue(expression)
                    }
                })
            }
        }
    }

    private fun dexSafeName(name: Name): Name {
        return if (name.isSpecial && name.asString().contains(' ')) {
            val sanitized = name
                .asString()
                .replace(' ', '$')
                .replace('<', '$')
                .replace('>', '$')
            Name.identifier(sanitized)
        } else name
    }

}
