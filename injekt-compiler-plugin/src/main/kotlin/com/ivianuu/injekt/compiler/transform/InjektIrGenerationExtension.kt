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

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class InjektIrGenerationExtension(private val project: Project) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val bindingTrace = DelegatingBindingTrace(
            pluginContext.bindingContext, "trace in " +
                    "ComposeIrGenerationExtension"
        )

        val declarationStore =
            InjektDeclarationStore(
                pluginContext,
                moduleFragment
            )

        fun IrElementTransformerVoid.visitModuleAndGenerateSymbols() {
            generateSymbols(pluginContext)
            visitModuleFragment(moduleFragment, null)
            generateSymbols(pluginContext)
        }

        ComponentAggregateGenerator(
            project,
            pluginContext
        ).visitModuleAndGenerateSymbols()

        val moduleAggregateGenerator = ModuleAggregateGenerator(
            project,
            pluginContext
        ).also { it.visitModuleAndGenerateSymbols() }

        // transform the config blocks of Component { ... } to a module
        ComponentBlockTransformer(
            pluginContext
        ).visitModuleAndGenerateSymbols()

        // transform all @Module fun module() { ... } to classes
        val moduleTransformer =
            ModuleTransformer(
                pluginContext,
                declarationStore,
                moduleFragment
            )

        // transform all Component { ... } calls to a Component implementation
        val componentTransformer =
            ComponentTransformer(
                pluginContext,
                declarationStore,
                moduleFragment
            )

        declarationStore.moduleAggregateGenerator = moduleAggregateGenerator
        declarationStore.componentTransformer = componentTransformer
        declarationStore.moduleTransformer = moduleTransformer

        moduleTransformer.visitModuleAndGenerateSymbols()
        componentTransformer.visitModuleAndGenerateSymbols()

        // transform component.get<String>() to component.get("java.lang.String")
        ComponentGetTransformer(
            pluginContext
        ).visitModuleAndGenerateSymbols()
    }

    val SymbolTable.allUnbound: List<IrSymbol>
        get() {
            val r = mutableListOf<IrSymbol>()
            r.addAll(unboundClasses)
            r.addAll(unboundConstructors)
            r.addAll(unboundEnumEntries)
            r.addAll(unboundFields)
            r.addAll(unboundSimpleFunctions)
            r.addAll(unboundProperties)
            r.addAll(unboundTypeParameters)
            r.addAll(unboundTypeAliases)
            return r
        }

    fun generateSymbols(pluginContext: IrPluginContext) {
        lateinit var unbound: List<IrSymbol>
        val visited = mutableSetOf<IrSymbol>()
        do {
            unbound = pluginContext.symbolTable.allUnbound

            for (symbol in unbound) {
                if (symbol in visited) {
                    continue
                }
                // Symbol could get bound as a side effect of deserializing other symbols.
                if (!symbol.isBound) {
                    pluginContext.irProviders.forEach { it.getDeclaration(symbol) }
                }
                if (!symbol.isBound) {
                    visited.add(symbol)
                }
            }
        } while ((unbound - visited).isNotEmpty())
    }
}
