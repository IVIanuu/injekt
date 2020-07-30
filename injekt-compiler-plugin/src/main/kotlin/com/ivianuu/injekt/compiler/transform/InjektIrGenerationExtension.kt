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

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.component.ComponentFactoryTransformer
import com.ivianuu.injekt.compiler.transform.component.ComponentIndexingTransformer
import com.ivianuu.injekt.compiler.transform.component.ComponentTransformer
import com.ivianuu.injekt.compiler.transform.component.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.component.EntryPointTransformer
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitCallTransformer
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextTransformer
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitIndexingTransformer
import com.ivianuu.injekt.compiler.transform.implicit.WithInstancesTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolTable

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val symbolRemapper = DeepCopySymbolRemapper()
        val injektPluginContext = InjektPluginContext(moduleFragment, pluginContext, symbolRemapper)

        EffectTransformer(injektPluginContext).doLower(moduleFragment)

        WithInstancesTransformer(
            injektPluginContext
        ).doLower(moduleFragment)

        ComponentFactoryTransformer(injektPluginContext).doLower(moduleFragment)

        val indexer = Indexer(
            injektPluginContext,
            moduleFragment,
            InjektSymbols(pluginContext)
        )

        val implicitContextParamTransformer =
            ImplicitContextTransformer(pluginContext, symbolRemapper)

        implicitContextParamTransformer.doLower(moduleFragment)

        ImplicitIndexingTransformer(pluginContext, indexer).doLower(moduleFragment)

        ImplicitCallTransformer(injektPluginContext).doLower(moduleFragment)

        val declarationGraph = DeclarationGraph(
            indexer,
            moduleFragment,
            injektPluginContext,
            implicitContextParamTransformer
        )

        EntryPointTransformer(injektPluginContext).doLower(moduleFragment)

        ComponentIndexingTransformer(indexer, injektPluginContext).doLower(moduleFragment)

        ComponentTransformer(
            injektPluginContext,
            declarationGraph
        ).doLower(
            moduleFragment
        )

        TmpMetadataPatcher(injektPluginContext).doLower(moduleFragment)

        generateSymbols(pluginContext)
    }

}

private val SymbolTable.allUnbound: List<IrSymbol>
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

private fun generateSymbols(pluginContext: IrPluginContext) {
    lateinit var unbound: List<IrSymbol>
    val visited = mutableSetOf<IrSymbol>()
    do {
        unbound = (pluginContext.symbolTable as SymbolTable).allUnbound

        for (symbol in unbound) {
            if (visited.contains(symbol)) {
                continue
            }
            // Symbol could get bound as a side effect of deserializing other symbols.
            if (!symbol.isBound) {
                (pluginContext as IrPluginContextImpl).linker.getDeclaration(symbol)
            }
            if (!symbol.isBound) { visited.add(symbol) }
        }
    } while ((unbound - visited).isNotEmpty())
}
