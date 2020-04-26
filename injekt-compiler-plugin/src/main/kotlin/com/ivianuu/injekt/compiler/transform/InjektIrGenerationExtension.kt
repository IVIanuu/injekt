package com.ivianuu.injekt.compiler.transform

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

        /*ClassProviderGenerator(pluginContext, project)
            .visitModuleAndGenerateSymbols()*/

        // generate bindings from binding definitions
        BindingIntrinsicTransformer(pluginContext)
            .visitModuleAndGenerateSymbols()

        /*InjektInitTransformer(pluginContext)
            .visitModuleAndGenerateSymbols()*/

        // rewrite calls like component.get<String>() -> component.get(keyOf<String>())
        KeyIntrinsicTransformer(
            pluginContext
        ).visitModuleAndGenerateSymbols()

        // cache static keyOf calls
        KeyCachingTransformer(pluginContext)
            .visitModuleAndGenerateSymbols()

        // rewrite keyOf<String>() -> keyOf(String::class)
        KeyOfTransformer(pluginContext).visitModuleAndGenerateSymbols()

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
