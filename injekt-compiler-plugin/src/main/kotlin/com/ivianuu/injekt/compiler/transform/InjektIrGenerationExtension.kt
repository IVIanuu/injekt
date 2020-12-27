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

import com.ivianuu.injekt.compiler.analysis.GivenFunFunctionDescriptor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        generateSymbols(pluginContext)
        moduleFragment.transform(GivenCallTransformer(pluginContext), null)
        moduleFragment.transform(GivenOptimizationTransformer(), null)
        moduleFragment.transform(GivenFunCallTransformer(pluginContext), null)
        moduleFragment.transform(KeyTypeParameterTransformer(pluginContext), null)
        moduleFragment.patchDeclarationParents()
        generateSymbols(pluginContext)
    }

    override fun resolveSymbol(
        symbol: IrSymbol,
        context: TranslationPluginContext
    ): IrDeclaration? {
        if (symbol.descriptor is GivenFunFunctionDescriptor) {
            return IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                symbol as IrSimpleFunctionSymbol,
                symbol.descriptor.name,
                symbol.descriptor.visibility,
                symbol.descriptor.modality,
                context.typeTranslator.translateType(symbol.descriptor.returnType!!),
                false,
                false,
                false,
                symbol.descriptor.isSuspend,
                false,
                false,
                false,
                false
            ).apply {
                (context.symbolTable as SymbolTable)
                    .declareSimpleFunction(symbol.descriptor) { this }
            }
        } else {
            return null
        }
    }

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

@OptIn(ObsoleteDescriptorBasedAPI::class)
@Suppress("UNUSED_PARAMETER", "DEPRECATION")
fun generateSymbols(pluginContext: IrPluginContext) {
    lateinit var unbound: List<IrSymbol>
    val visited = mutableSetOf<IrSymbol>()
    do {
        unbound = (pluginContext.symbolTable as SymbolTable).allUnbound

        for (symbol in unbound) {
            println("lol $symbol ${symbol.descriptor}")
            if (visited.contains(symbol)) {
                continue
            }
            // Symbol could get bound as a side effect of deserializing other symbols.
            if (!symbol.isBound) {
                (pluginContext as IrPluginContextImpl).linker.getDeclaration(symbol)
            }
            if (!symbol.isBound) {
                visited.add(symbol)
            }
        }
    } while ((unbound - visited).isNotEmpty())
}