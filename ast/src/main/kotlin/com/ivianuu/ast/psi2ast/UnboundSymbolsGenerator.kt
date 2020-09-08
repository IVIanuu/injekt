package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.symbols.AstSymbol
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

fun DescriptorSymbolTable.generateUnboundSymbols(
    stubGenerator: DeclarationStubGenerator
) {
    lateinit var unbound: Map<DeclarationDescriptor, AstSymbol<*>>
    val visited = mutableSetOf<AstSymbol<*>>()
    do {
        unbound = unboundSymbols
        for ((descriptor, symbol) in unbound) {
            if (visited.contains(symbol)) {
                continue
            }
            // Symbol could get bound as a side effect of deserializing other symbols.
            if (!symbol.isBound) {
                stubGenerator.getDeclaration(descriptor)
            }
            if (!symbol.isBound) { visited.add(symbol) }
        }
    } while ((unbound.filter { it.value !in visited }).isNotEmpty())
    check(unboundSymbols.isEmpty()) {
        "Expected no unbound symbols but was $unboundSymbols"
    }
}