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

import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

abstract class AbstractFunctionTransformer(
    pluginContext: IrPluginContext,
    private val transformOrder: TransformOrder
) : AbstractInjektTransformer(pluginContext) {

    protected val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    protected val decoys = mutableMapOf<IrFunction, IrFunction>()

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

    protected abstract fun transform(
        function: IrFunction,
        callback: (IrFunction) -> Unit
    )

    protected abstract fun transformExternal(
        function: IrFunction,
        callback: (IrFunction) -> Unit
    )

    protected abstract fun createDecoy(
        original: IrFunction,
        transformed: IrFunction
    ): IrFunction

    protected fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        decoys[function]?.let { return it }

        if (!needsTransform(function)) return function

        val callback: (IrFunction) -> Unit = {
            transformedFunctions[function] = it
        }

        if (function.isExternalDeclaration())
            transformExternal(function, callback) else transform(function, callback)

        return transformedFunctions.getValue(function)
    }

    enum class TransformOrder {
        BottomUp, TopDown
    }

}
