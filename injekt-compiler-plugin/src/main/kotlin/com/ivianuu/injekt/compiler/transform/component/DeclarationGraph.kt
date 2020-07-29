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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextTransformer
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val pluginContext: IrPluginContext,
    private val implicitTransformer: ImplicitContextTransformer
) {

    private val _rootComponentFactories = mutableListOf<IrClass>()
    val rootComponentFactories: List<IrClass> get() = _rootComponentFactories

    private val _bindings = mutableListOf<IrFunction>()
    val bindings: List<IrFunction> get() = _bindings

    val entryPoints: List<IrClass> get() = _entryPoints
    private val _entryPoints = mutableListOf<IrClass>()

    private val _mapEntries = mutableListOf<IrFunction>()
    val mapEntries: List<IrFunction> get() = _mapEntries

    private val _setElements = mutableListOf<IrFunction>()
    val setElements: List<IrFunction> get() = _setElements

    fun getAllContextsForFunction(function: IrFunction): List<IrClass> {
        val contexts = mutableListOf<IrClass>()

        contexts += function.getContext()!!

        fun collectAllContextsForFunction(function: IrFunction) {
            indexer.indices
                .mapNotNull { pluginContext.referenceClass(it)?.owner }
                .mapNotNull { it.getAnnotation(InjektFqNames.ReaderImpl) }
                .filter { annotation ->
                    annotation.getValueArgument(0)
                        .let { it as IrConst<String> }
                        .value == function.uniqueName()
                }
                .map {
                    it.getValueArgument(1)
                        .let { it as IrConst<String> }
                        .value
                }
                .flatMapFix { implName ->
                    pluginContext.referenceFunctions(
                        FqName(
                            implName.replaceAfter("__", "")
                                .replace("__", "")
                        )
                    )
                        .filter {
                            it.owner.uniqueName() == implName
                        }
                }
                .map { implicitTransformer.getTransformedFunction(it.owner) }
                .forEach {
                    contexts += it.getContext()!!
                    collectAllContextsForFunction(it)
                }
        }

        collectAllContextsForFunction(function)

        return contexts
    }

    fun initialize() {
        collectRootComponentFactories()
        collectEntryPoints()
        collectBindings()
        collectMapEntries()
        collectSetElements()
    }

    private fun collectRootComponentFactories() {
        indexer.indices
            .mapNotNull { pluginContext.referenceClass(it)?.owner }
            .filter { it.hasAnnotation(InjektFqNames.RootComponentFactory) }
            .forEach { _rootComponentFactories += it }
    }

    private fun collectEntryPoints() {
        indexer.indices
            .mapNotNull { pluginContext.referenceClass(it)?.owner }
            .filter { it.hasAnnotation(InjektFqNames.EntryPoint) }
            .forEach { _entryPoints += it }
    }

    private fun collectBindings() {
        indexer.indices
            .flatMapFix {
                pluginContext.referenceFunctions(it)
                    .map { it.owner } +
                        (pluginContext.referenceClass(it)?.constructors
                            ?.map { it.owner }
                            ?.toList() ?: emptyList()) +
                        pluginContext.referenceProperties(it)
                            .mapNotNull { it.owner.getter }
            }
            .filter {
                it.hasAnnotation(InjektFqNames.Given) ||
                        (it is IrConstructor && it.constructedClass.hasAnnotation(InjektFqNames.Given)) ||
                        (it is IrSimpleFunction && it.correspondingPropertySymbol?.owner?.hasAnnotation(
                            InjektFqNames.Given
                        ) == true)
            }
            .map { implicitTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _bindings += it }
    }

    private fun collectMapEntries() {
        indexer.indices
            .flatMapFix { pluginContext.referenceFunctions(it) }
            .map { it.owner }
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { implicitTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _mapEntries += it }
    }

    private fun collectSetElements() {
        indexer.indices
            .flatMapFix { pluginContext.referenceFunctions(it) }
            .map { it.owner }
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { implicitTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _setElements += it }
    }

}
