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
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.readercontextimpl.ReaderContextImplContext
import com.ivianuu.injekt.compiler.backend.readercontextimpl.RootContextImplTransformer
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

@Given
class InjektIrGenerationExtension : IrGenerationExtension {

    private lateinit var irContext: IrContext

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println(moduleFragment.dumpSrc())
        irContext = childContext(moduleFragment, pluginContext)
        irContext.runReader {
            given<ReaderContextParamTransformer>().lower()
            given<ReaderCallTransformer>().lower()

            val initTrigger = getInitTrigger()
            if (initTrigger != null) {
                childContext<ReaderContextImplContext>(initTrigger).runReader {
                    given<RootContextImplTransformer>().lower()
                }
            }

            generateSymbols()
        }
        println(moduleFragment.dumpSrc())
    }

}

typealias InitTrigger = IrDeclarationWithName

@Reader
private fun getInitTrigger(): InitTrigger? {
    var initTrigger: InitTrigger? = null

    irModule.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
        override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
            if (declaration.hasAnnotation(InjektFqNames.InitializeInjekt)) {
                initTrigger = declaration as IrDeclarationWithName
                return declaration
            }
            return super.visitDeclaration(declaration)
        }
    })

    return initTrigger
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
