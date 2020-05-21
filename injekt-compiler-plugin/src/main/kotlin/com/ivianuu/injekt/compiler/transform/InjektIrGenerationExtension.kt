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
import com.ivianuu.injekt.compiler.transform.annotatedclass.ClassFactoryTransformer
import com.ivianuu.injekt.compiler.transform.annotatedclass.MembersInjectorTransformer
import com.ivianuu.injekt.compiler.transform.composition.BindingAdapterTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionAggregateGenerator
import com.ivianuu.injekt.compiler.transform.composition.CompositionEntryPointsTransformer
import com.ivianuu.injekt.compiler.transform.composition.CompositionFactoryParentTransformer
import com.ivianuu.injekt.compiler.transform.composition.EntryPointOfTransformer
import com.ivianuu.injekt.compiler.transform.composition.GenerateCompositionsTransformer
import com.ivianuu.injekt.compiler.transform.composition.InlineObjectGraphCallTransformer
import com.ivianuu.injekt.compiler.transform.composition.ObjectGraphCallTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.InlineFactoryTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.InlineModuleLambdaTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleClassTransformer
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

        val moduleFunctionTransformer = ModuleFunctionTransformer(pluginContext)

        val moduleClassTransformer = ModuleClassTransformer(
            pluginContext,
            declarationStore,
            moduleFunctionTransformer
        ).also { declarationStore.moduleClassTransformer = it }

        val factoryModuleTransformer = FactoryModuleTransformer(
            pluginContext,
            moduleFunctionTransformer
        ).also { declarationStore.factoryModuleTransformer = it }
        val factoryTransformer = RootFactoryTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.factoryTransformer = it }

        // write qualifiers of expression to the irTrace
        QualifiedMetadataTransformer(pluginContext).lower(moduleFragment)

        if (pluginContext.androidEnabled) {
            AndroidEntryPointTransformer(pluginContext).lower(moduleFragment)
        }

        if (pluginContext.compositionsEnabled) {
            InlineObjectGraphCallTransformer(pluginContext).lower(moduleFragment)

            ObjectGraphCallTransformer(pluginContext).lower(moduleFragment)

            BindingAdapterTransformer(pluginContext).lower(moduleFragment)

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
        MembersInjectorTransformer(pluginContext)
            .also { declarationStore.membersInjectorTransformer = it }
            .lower(moduleFragment)

        // generate a factory for each annotated class
        ClassFactoryTransformer(pluginContext, declarationStore)
            .also { declarationStore.classFactoryTransformer = it }
            .lower(moduleFragment)

        // add a local @Factory fun at each call side of a inline factory
        InlineFactoryTransformer(pluginContext, declarationStore)
            .lower(moduleFragment)

        // move the module block of a @Factory function to a seperate @Module function
        factoryModuleTransformer.lower(moduleFragment)

        // transform inline @Module calls to pass data around
        InlineModuleLambdaTransformer(pluginContext)
            .lower(moduleFragment)

        ProviderDslFunctionTransformer(pluginContext).lower(moduleFragment)

        ClassOfFunctionTransformer(pluginContext).lower(moduleFragment)

        // transform @Module functions
        moduleFunctionTransformer.lower(moduleFragment)

        // generate a Module class for each @Module function
        moduleClassTransformer.lower(moduleFragment)

        // generate factory implementations
        factoryTransformer.lower(moduleFragment)

        // patch file metadata
        FileMetadataPatcher(pluginContext).lower(moduleFragment)
    }

}
