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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.implicit.GenericContextImplTransformer
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitCallTransformer
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.transform.implicit.ReaderTrackingTransformer
import com.ivianuu.injekt.compiler.transform.runreader.ComponentIndexingTransformer
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderContextImplTransformer
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        var initializeInjekt = false
        var initFile: IrFile? = null

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.InitializeInjekt)) {
                    initializeInjekt = true
                    initFile = currentFile
                }
                return super.visitDeclaration(declaration)
            }
        })

        val injektPluginContext = InjektPluginContext(moduleFragment, pluginContext)

        EffectTransformer(injektPluginContext).doLower(moduleFragment)

        val indexer = Indexer(
            injektPluginContext,
            moduleFragment,
            InjektSymbols(injektPluginContext)
        )

        val implicitContextParamTransformer =
            ImplicitContextParamTransformer(injektPluginContext, indexer)
        implicitContextParamTransformer.doLower(moduleFragment)

        ImplicitCallTransformer(injektPluginContext, indexer).doLower(moduleFragment)

        ReaderTrackingTransformer(
            injektPluginContext,
            indexer,
            implicitContextParamTransformer
        ).doLower(moduleFragment)

        RunReaderTransformer(pluginContext, indexer).doLower(moduleFragment)

        val declarationGraph =
            DeclarationGraph(
                indexer,
                moduleFragment,
                implicitContextParamTransformer
            )

        ComponentIndexingTransformer(indexer, injektPluginContext).doLower(moduleFragment)

        if (initializeInjekt) {
            declarationGraph.initialize()
            RunReaderContextImplTransformer(
                pluginContext,
                declarationGraph,
                implicitContextParamTransformer,
                initFile!!
            ).doLower(moduleFragment)
            GenericContextImplTransformer(pluginContext, declarationGraph)
                .doLower(moduleFragment)
        }

        TmpMetadataPatcher(injektPluginContext).doLower(moduleFragment)

        generateSymbols(pluginContext)

        //println(moduleFragment.dumpSrc())
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
