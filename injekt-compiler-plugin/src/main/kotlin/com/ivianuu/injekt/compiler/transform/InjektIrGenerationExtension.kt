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

import com.ivianuu.injekt.compiler.compositionsEnabled
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.transform.composition.BindingEffectTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionFactoryParentTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionMetadataTransformer
import com.ivianuu.injekt.compiler.transform.composition.EntryPointOfTransformer
import com.ivianuu.injekt.compiler.transform.composition.GenerateCompositionsTransformer
import com.ivianuu.injekt.compiler.transform.reader.ReaderTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionComponentReaderCallTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleFunctionTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.name.FqName

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val symbolRemapper = DeepCopySymbolRemapper()

        val pluginContext = object : IrPluginContext by pluginContext {
            override fun referenceClass(fqName: FqName): IrClassSymbol? {
                return pluginContext.referenceClass(fqName)
                    ?.let { symbolRemapper.getReferencedClass(it) }
            }

            override fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> {
                return pluginContext.referenceConstructors(classFqn)
                    .map { symbolRemapper.getReferencedConstructor(it) }
            }

            override fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> {
                return pluginContext.referenceFunctions(fqName)
                    .map { symbolRemapper.getReferencedSimpleFunction(it) }
            }

            override fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> {
                return pluginContext.referenceProperties(fqName)
                    .map { symbolRemapper.getReferencedProperty(it) }
            }
        }

        val declarationStore = InjektDeclarationStore(pluginContext)

        val readerFunctionTransformer = ReaderTransformer(pluginContext, symbolRemapper)
            .also { declarationStore.readerTransformer = it }
        readerFunctionTransformer.lower(moduleFragment)

        val moduleFunctionTransformer = ModuleFunctionTransformer(pluginContext, declarationStore)
            .also { declarationStore.moduleFunctionTransformer = it }
        val factoryModuleTransformer = FactoryModuleTransformer(
            pluginContext
        ).also { declarationStore.factoryModuleTransformer = it }
        val factoryTransformer = RootFactoryTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.factoryTransformer = it }

        if (pluginContext.compositionsEnabled) {
            BindingEffectTransformer(pluginContext).lower(moduleFragment)

            CompositionComponentReaderCallTransformer(
                pluginContext
            ).lower(moduleFragment)

            // generate a @Module entryPointModule() { entryPoint<T>() } module at each call site of entryPointOf<T>()
            EntryPointOfTransformer(pluginContext).lower(moduleFragment)

            // add @Parents annotation to @CompositionFactory functions
            CompositionFactoryParentTransformer(pluginContext)
                .lower(moduleFragment)

            CompositionMetadataTransformer(pluginContext)
                .also { declarationStore.compositionMetadataTransformer = it }
                .lower(moduleFragment)

            // generate composition factories
            GenerateCompositionsTransformer(
                pluginContext, declarationStore
            ).lower(moduleFragment)
        }

        // move the module block of a @Factory function to a separate @Module function
        factoryModuleTransformer.lower(moduleFragment)

        // transform @Module functions
        moduleFunctionTransformer.lower(moduleFragment)

        readerFunctionTransformer.addContextClassesToFiles()

        // generate factory implementations
        factoryTransformer.lower(moduleFragment)

        // patch metadata
        TmpMetadataPatcher(pluginContext).lower(moduleFragment)

        //println(moduleFragment.dumpSrc())
    }

}
