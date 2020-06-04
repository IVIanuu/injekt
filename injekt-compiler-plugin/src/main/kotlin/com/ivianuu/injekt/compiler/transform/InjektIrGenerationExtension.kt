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

import com.ivianuu.injekt.compiler.androidEnabled
import com.ivianuu.injekt.compiler.compositionsEnabled
import com.ivianuu.injekt.compiler.transform.android.AndroidEntryPointTransformer
import com.ivianuu.injekt.compiler.transform.android.CompositionAndroidAppTransformer
import com.ivianuu.injekt.compiler.transform.composition.BindingEffectTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionAggregateGenerator
import com.ivianuu.injekt.compiler.transform.composition.CompositionEntryPointsTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionFactoryParentTransformer
import com.ivianuu.injekt.compiler.transform.composition.EntryPointOfTransformer
import com.ivianuu.injekt.compiler.transform.composition.GenerateCompositionsTransformer
import com.ivianuu.injekt.compiler.transform.composition.ObjectGraphCallTransformer
import com.ivianuu.injekt.compiler.transform.composition.ObjectGraphFunctionTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleFunctionTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class InjektIrGenerationExtension(
    private val project: Project
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val declarationStore = InjektDeclarationStore(pluginContext)

        val moduleFunctionTransformer = ModuleFunctionTransformer(pluginContext, declarationStore)
            .also { declarationStore.moduleFunctionTransformer = it }

        val factoryModuleTransformer = FactoryModuleTransformer(
            pluginContext
        ).also { declarationStore.factoryModuleTransformer = it }
        val factoryTransformer = RootFactoryTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.factoryTransformer = it }

        if (pluginContext.androidEnabled) {
            CompositionAndroidAppTransformer(pluginContext).lower(moduleFragment)
            AndroidEntryPointTransformer(pluginContext).lower(moduleFragment)
        }

        if (pluginContext.compositionsEnabled) {
            ObjectGraphFunctionTransformer(pluginContext).lower(moduleFragment)

            ObjectGraphCallTransformer(pluginContext).lower(moduleFragment)

            BindingEffectTransformer(pluginContext).lower(moduleFragment)

            // generate a @Module entryPointModule() { entryPoint<T>() } module at each call site of entryPointOf<T>()
            EntryPointOfTransformer(pluginContext).lower(moduleFragment)

            val compositionAggregateGenerator =
                CompositionAggregateGenerator(pluginContext, project)
                    .also { it.lower(moduleFragment) }

            // add @Parents annotation to @CompositionFactory functions
            CompositionFactoryParentTransformer(pluginContext)
                .lower(moduleFragment)

            CompositionEntryPointsTransformer(pluginContext)
                .lower(moduleFragment)

            // generate composition factories
            GenerateCompositionsTransformer(
                pluginContext, declarationStore,
                compositionAggregateGenerator
            ).lower(moduleFragment)
        }

        // generate a members injector for each annotated class
        MembersInjectorTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.membersInjectorTransformer = it }
            .lower(moduleFragment)

        // move the module block of a @Factory function to a separate @Module function
        factoryModuleTransformer.lower(moduleFragment)

        // transform @Module functions
        moduleFunctionTransformer.lower(moduleFragment)

        // generate factory implementations
        factoryTransformer.lower(moduleFragment)

        // patch metadata
        TmpMetadataPatcher(pluginContext).lower(moduleFragment)

        //println(moduleFragment.dumpSrc())
    }

}
