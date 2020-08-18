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

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        /*val injektContext = InjektContext(pluginContext, moduleFragment)
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

        if (injektContext.referenceClass(InjektFqNames.Effect) != null) {
            EffectTransformer(injektContext).doLower(moduleFragment)
        }

        val indexer = Indexer(
            injektContext,
            moduleFragment,
            InjektSymbols(injektContext)
        )

        ReaderContextCallTransformer(
            injektContext,
            indexer
        ).doLower(moduleFragment)

        val implicitContextParamTransformer =
            ImplicitContextParamTransformer(injektContext, indexer)
        implicitContextParamTransformer.doLower(moduleFragment)

        ImplicitCallTransformer(injektContext, indexer).doLower(moduleFragment)

        ReaderTrackingTransformer(
            injektContext,
            indexer,
            implicitContextParamTransformer
        ).doLower(moduleFragment)

        RunReaderCallTransformer(
            injektContext,
            indexer
        ).doLower(moduleFragment)

        IndexingTransformer(
            indexer,
            injektContext
        ).doLower(moduleFragment)

        if (initializeInjekt) {
            val declarationGraph = DeclarationGraph(
                indexer,
                moduleFragment,
                implicitContextParamTransformer
            )
            ReaderContextImplTransformer(
                injektContext,
                declarationGraph,
                implicitContextParamTransformer,
                initFile!!
            ).doLower(moduleFragment)
            GenericContextImplTransformer(
                injektContext,
                declarationGraph,
                initFile!!
            ).doLower(moduleFragment)
        }

        TmpMetadataPatcher(injektContext).doLower(moduleFragment)

        generateSymbols(pluginContext)*/
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
