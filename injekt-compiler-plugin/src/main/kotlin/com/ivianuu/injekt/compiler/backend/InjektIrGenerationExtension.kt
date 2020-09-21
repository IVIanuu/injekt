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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

@Given
class InjektIrGenerationExtension : IrGenerationExtension {

    private lateinit var irContext: IrContext

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println(moduleFragment.dumpSrc())
        irContext = childContext(moduleFragment, pluginContext)
        irContext.runReader {
            given<ReaderContextParamTransformer>().lower()
            given<ReaderCallTransformer>().lower()

            /*val initTrigger = getInitTrigger()
            if (initTrigger != null) {
                childContext<ReaderContextImplContext>(initTrigger).runReader {
                    given<ReaderContextImplTransformer>().lower()
                }
            }
*/
            generateSymbols()
        }
        println(moduleFragment.dumpSrc())
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

@Reader
private fun generateSymbols() {
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
