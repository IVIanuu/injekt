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
import com.ivianuu.injekt.compiler.transform.runreader.BindingIndexingTransformer
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderCallTransformer
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderContextImplTransformer
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
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import kotlin.system.measureTimeMillis

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val injektContext = InjektContext(pluginContext, moduleFragment)
        var initializeInjekt = false
        var initFile: IrFile? = null

        fun AbstractInjektTransformer.doLowerAndMeasure() {
            val duration = measureTimeMillis { doLower(moduleFragment) }
            println("${javaClass.name} took $duration ms")
        }

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
            EffectTransformer(injektContext).doLowerAndMeasure()
        }

        val indexer = Indexer(
            injektContext,
            moduleFragment,
            InjektSymbols(injektContext)
        )

        val implicitContextParamTransformer =
            ImplicitContextParamTransformer(injektContext, indexer)
        implicitContextParamTransformer.doLowerAndMeasure()

        ImplicitCallTransformer(injektContext, indexer).doLowerAndMeasure()

        ReaderTrackingTransformer(
            injektContext,
            indexer,
            implicitContextParamTransformer
        ).doLowerAndMeasure()

        RunReaderCallTransformer(injektContext, indexer).doLowerAndMeasure()

        BindingIndexingTransformer(indexer, injektContext).doLowerAndMeasure()

        if (initializeInjekt) {
            val (initTime, declarationGraph) =
                measureTimeMillisWithResult {
                    DeclarationGraph(
                        indexer,
                        moduleFragment,
                        implicitContextParamTransformer
                    )
                }
            println("Initializing graph took $initTime ms")
            val runReaderContextImplTransformer = RunReaderContextImplTransformer(
                injektContext,
                declarationGraph,
                implicitContextParamTransformer,
                initFile!!
            )
            declarationGraph.runReaderContextImplTransformer = runReaderContextImplTransformer
            runReaderContextImplTransformer.doLowerAndMeasure()
            GenericContextImplTransformer(
                injektContext,
                declarationGraph,
                runReaderContextImplTransformer,
                initFile!!
            )
                .doLowerAndMeasure()
        }

        TmpMetadataPatcher(injektContext).doLowerAndMeasure()

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
