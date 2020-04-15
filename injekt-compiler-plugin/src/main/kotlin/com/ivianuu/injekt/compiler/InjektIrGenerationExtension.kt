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
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class InjektIrGenerationExtension(private val project: Project) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        fun IrElementTransformerVoid.visitModuleAndGenerateSymbols() {
            generateSymbols(pluginContext)
            visitModuleFragment(moduleFragment, null)
            generateSymbols(pluginContext)
        }

        // replace intrinsics with names
        DeclarationNameIntrinsicTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // generate modules for bindings
        BindingModuleGenerator(pluginContext).visitModuleAndGenerateSymbols()

        // generate accessors for each module
        ModuleAccessorGenerator(pluginContext).visitModuleAndGenerateSymbols()

        // generate metadata classes in the aggregate package
        // which allows to access all classes even from different compilations
        AggregateGenerator(pluginContext, project).visitModuleAndGenerateSymbols()

        // transform initializeEndpoint calls
        InjektInitTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // transform binding provider lambdas to classes
        // to allow further transformations
        BindingProviderLambdaToClassTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // rewrite key overload stub calls to the real calls
        KeyOverloadTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // memoize static keyOf calls
        KeyCachingTransformer(pluginContext).visitModuleAndGenerateSymbols()
        // rewrite keyOf<String>() -> keyOf(String::class)
        KeyOfTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // cache providers
        BindingProviderCachingTransformer(pluginContext).visitModuleAndGenerateSymbols()
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
                if (visited.contains(symbol)) {
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
