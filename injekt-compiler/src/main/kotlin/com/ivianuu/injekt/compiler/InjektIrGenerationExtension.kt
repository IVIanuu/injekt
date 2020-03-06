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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InjektIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // create a symbol remapper to be used across all transforms
        val symbolRemapper = DeepCopySymbolRemapper()

        val transformers = listOf(
            InjektBindingGenerator(pluginContext)/*,
            TypedFunctionMarker(pluginContext),
            TypeOfParamsTransformer(pluginContext, symbolRemapper),
            TypedCallTransformer(pluginContext)*/
        )

        transformers.forEach {
            ExternalDependenciesGenerator(pluginContext.symbolTable, pluginContext.irProviders)
                .generateUnboundSymbolsAsDependencies()
            moduleFragment.transformChildrenVoid(it)
        }
    }
}