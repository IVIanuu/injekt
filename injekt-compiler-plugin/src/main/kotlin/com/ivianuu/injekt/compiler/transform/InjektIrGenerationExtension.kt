package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InjektIrGenerationExtension(
    private val declarationStore: DeclarationStore,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        generateSymbols(pluginContext)
        moduleFragment.transformChildrenVoid(GivenCallTransformer(declarationStore, pluginContext))
        moduleFragment.transformChildrenVoid(GivenOptimizationTransformer())
        generateSymbols(pluginContext)
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