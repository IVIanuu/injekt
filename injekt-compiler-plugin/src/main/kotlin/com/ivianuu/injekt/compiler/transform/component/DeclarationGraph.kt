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
import com.ivianuu.injekt.compiler.indexPackageFile
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform.ReaderTransformer
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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationGraph(
    private val module: IrModuleFragment,
    private val pluginContext: IrPluginContext,
    private val readerTransformer: ReaderTransformer
) {

    private val _componentFactories = mutableListOf<ComponentFactory>()
    val componentFactories: List<ComponentFactory> get() = _componentFactories

    private val _bindings = mutableListOf<Binding>()
    val bindings: List<Binding> get() = _bindings

    val entryPoints: List<EntryPoint> get() = _entryPoints
    private val _entryPoints = mutableListOf<EntryPoint>()

    private val _mapEntries = mutableListOf<MapEntries>()
    val mapEntries: List<MapEntries> get() = _mapEntries

    private val _setElements = mutableListOf<SetElements>()
    val setElements: List<SetElements> get() = _setElements

    private val indices by lazy {
        val memberScope = pluginContext.moduleDescriptor.getPackage(InjektFqNames.IndexPackage)
            .memberScope

        (module.indexPackageFile.declarations
            .filterIsInstance<IrClass>() +
                (memberScope.getClassifierNames() ?: emptySet()).mapNotNull {
                    memberScope.getContributedClassifier(
                        it,
                        NoLookupLocation.FROM_BACKEND
                    )
                }.map { pluginContext.referenceClass(it.fqNameSafe)!!.owner })
            .map {
                it.getAnnotation(InjektFqNames.Index)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value
                    .let { FqName(it) }
            } +
                // this is a workaround for cleaner tests
                FqName("com.ivianuu.injekt.test.TestComponent.Factory") +
                FqName("com.ivianuu.injekt.test.TestParentComponent.Factory") +
                FqName("com.ivianuu.injekt.test.TestChildComponent.Factory")
    }

    fun initialize() {
        collectComponentFactories()
        collectEntryPoints()
        collectBindings()
        collectMapEntries()
        collectSetElements()
    }

    private fun collectComponentFactories() {
        indices
            .mapNotNull { pluginContext.referenceClass(it)?.owner }
            .filter { it.hasAnnotation(InjektFqNames.ComponentFactory) }
            .forEach { _componentFactories += ComponentFactory(it) }
    }

    private fun collectEntryPoints() {
        indices
            .mapNotNull { pluginContext.referenceClass(it)?.owner }
            .filter { it.hasAnnotation(InjektFqNames.EntryPoint) }
            .forEach { _entryPoints += EntryPoint(it) }
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
            .map { readerTransformer.getTransformedFunction(it) }
            .distinct()
            .forEach { _bindings += Binding(it) }
    }

    private fun collectMapEntries() {
        indices
            .flatMapFix { pluginContext.referenceFunctions(it) }
            .map { it.owner }
            .map {
                if (it.isExternalDeclaration()) readerTransformer.getTransformedFunction(it)
                else it
            }
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { readerTransformer.getTransformedFunction(it) }
            .distinct()
            .forEach { _mapEntries += MapEntries(it) }
    }

    private fun collectSetElements() {
        indices
            .flatMapFix { pluginContext.referenceFunctions(it) }
            .map { it.owner }
            .map {
                if (it.isExternalDeclaration()) readerTransformer.getTransformedFunction(it)
                else it
            }
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { readerTransformer.getTransformedFunction(it) }
            .distinct()
            .forEach { _setElements += SetElements(it) }
    }

}

class ComponentFactory(
    val factory: IrClass
)

class Binding(
    val function: IrFunction
)

class EntryPoint(
    val entryPoint: IrClass
)

class SetElements(
    val function: IrFunction
)

class MapEntries(
    val function: IrFunction
)
