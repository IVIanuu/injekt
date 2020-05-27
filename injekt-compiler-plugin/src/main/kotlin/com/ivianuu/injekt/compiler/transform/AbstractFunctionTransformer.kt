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
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils

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
        val originalFunctions = mutableListOf<IrFunction>()
        val originalProperties = mutableListOf<Pair<IrProperty, IrSimpleFunction>>()
        loop@ for (child in declaration.declarations) {
            when (child) {
                is IrFunction -> originalFunctions.add(child)
                is IrProperty -> {
                    val getter = child.getter ?: continue@loop
                    originalProperties.add(child to getter)
                }
            }
        }
        val result = super.visitFile(declaration)
        result.patchWithDecoys(originalFunctions, originalProperties)
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val originalFunctions = mutableListOf<IrFunction>()
        val originalProperties = mutableListOf<Pair<IrProperty, IrSimpleFunction>>()
        loop@ for (child in declaration.declarations) {
            when (child) {
                is IrFunction -> originalFunctions.add(child)
                is IrProperty -> {
                    val getter = child.getter ?: continue@loop
                    originalProperties.add(child to getter)
                }
            }
        }
        val result = super.visitClass(declaration) as IrClass
        result.patchWithDecoys(originalFunctions, originalProperties)
        return result
    }

    private fun IrDeclarationContainer.patchWithDecoys(
        originalFunctions: List<IrFunction>,
        originalProperties: List<Pair<IrProperty, IrSimpleFunction>>
    ) {
        for (function in originalFunctions) {
            val transformed = transformedFunctions[function]
            if (transformed != null && transformed != function) {
                declarations.add(createDecoy(function, transformed))
            }
        }
        for ((property, getter) in originalProperties) {
            val transformed = transformedFunctions[getter]
            if (transformed != null && transformed != getter) {
                val newGetter = property.getter
                property.getter = (createDecoy(getter, transformed) as IrSimpleFunction)
                    .also { it.parent = this }
                declarations.add(newGetter!!)
                newGetter.parent = this
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
            try {
                copyTypeAndValueArgumentsFrom(expression)
            } catch (e: Throwable) {
                error("could not transform ${expression.dump()} to new function ${transformed.dump()}")
            }
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

        if (transformedFunction != function) {
            fun jvmNameAnnotation(name: String): IrConstructorCall {
                val jvmName = pluginContext.referenceClass(DescriptorUtils.JVM_NAME)!!
                return DeclarationIrBuilder(pluginContext, transformedFunction.symbol).run {
                    irCall(jvmName.constructors.single()).apply {
                        putValueArgument(0, irString(name))
                    }
                }
            }

            if (transformedFunction is IrSimpleFunction && function is IrSimpleFunction) {
                transformedFunction.correspondingPropertySymbol =
                    function.correspondingPropertySymbol
            }

            val descriptor = function.descriptor
            if (descriptor is PropertyGetterDescriptor &&
                !transformedFunction.hasAnnotation(DescriptorUtils.JVM_NAME)
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                transformedFunction.annotations += jvmNameAnnotation(name)
            }

            if (descriptor is PropertySetterDescriptor &&
                !transformedFunction.hasAnnotation(DescriptorUtils.JVM_NAME)
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                transformedFunction.annotations += jvmNameAnnotation(name)
            }
        }

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
