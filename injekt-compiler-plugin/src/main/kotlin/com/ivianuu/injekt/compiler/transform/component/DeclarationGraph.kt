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
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationGraph(
    val module: IrModuleFragment,
    private val pluginContext: IrPluginContext,
    private val implicitTransformer: ImplicitTransformer
) {

    private val _rootComponentFactories = mutableListOf<IrClass>()
    val rootComponentFactories: List<IrClass> get() = _rootComponentFactories

    private val _bindings = mutableListOf<ImplicitPair>()
    val bindings: List<ImplicitPair> get() = _bindings

    val entryPoints: List<IrClass> get() = _entryPoints
    private val _entryPoints = mutableListOf<IrClass>()

    private val _mapEntries = mutableListOf<ImplicitPair>()
    val mapEntries: List<ImplicitPair> get() = _mapEntries

    private val _setElements = mutableListOf<ImplicitPair>()
    val setElements: List<ImplicitPair> get() = _setElements

    val indices: List<FqName> by lazy {
        val memberScope = pluginContext.moduleDescriptor.getPackage(InjektFqNames.IndexPackage)
            .memberScope

        ((module.files
            .filter { it.fqName == InjektFqNames.IndexPackage }
            .flatMapFix { it.declarations }
            .filterIsInstance<IrClass>()) + ((memberScope.getClassifierNames()
            ?: emptySet()).mapNotNull {
            memberScope.getContributedClassifier(
                it,
                NoLookupLocation.FROM_BACKEND
            )
        }.map { pluginContext.referenceClass(it.fqNameSafe)!!.owner }))
            .map {
                it.getAnnotation(InjektFqNames.Index)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value
                    .let { FqName(it) }
            }
    }

    fun initialize() {
        collectRootComponentFactories()
        collectEntryPoints()
        collectBindings()
        collectMapEntries()
        collectSetElements()
    }

    private fun collectRootComponentFactories() {
        indices
            .mapNotNull { pluginContext.referenceClass(it)?.owner }
            .filter { it.hasAnnotation(InjektFqNames.RootComponentFactory) }
            .forEach { _rootComponentFactories += it }
    }

    private fun collectEntryPoints() {
        indices
            .mapNotNull { pluginContext.referenceClass(it)?.owner }
            .filter { it.hasAnnotation(InjektFqNames.EntryPoint) }
            .forEach { _entryPoints += it }
    }

    private fun collectBindings() {
        indices
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
            .map {
                ImplicitPair(
                    implicitTransformer.getTransformedFunction(it),
                    implicitTransformer.getReaderSignature(it)
                )
            }
            .onEach {
                println("found ${it.function} ${it.function.render()}")
            }
            .distinct()
            .forEach { _bindings += it }
    }

    private fun collectMapEntries() {
        indices
            .flatMapFix { pluginContext.referenceFunctions(it) }
            .map { it.owner }
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map {
                ImplicitPair(
                    implicitTransformer.getTransformedFunction(it),
                    implicitTransformer.getReaderSignature(it)
                )
            }
            .distinct()
            .forEach { _mapEntries += it }
    }

    private fun collectSetElements() {
        indices
            .flatMapFix { pluginContext.referenceFunctions(it) }
            .map { it.owner }
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map {
                ImplicitPair(
                    implicitTransformer.getTransformedFunction(it),
                    implicitTransformer.getReaderSignature(it)
                )
            }
            .distinct()
            .forEach { _setElements += it }
    }

    data class ImplicitPair(
        val function: IrFunction,
        val signature: IrFunction
    )

}
