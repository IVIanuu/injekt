package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstResolvedNamedReference
import com.ivianuu.ast.references.impl.AstResolvedNamedReferenceImpl
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedNamedReferenceBuilder {
    lateinit var name: Name
    lateinit var resolvedSymbol: AbstractAstBasedSymbol<*>

    fun build(): AstResolvedNamedReference {
        return AstResolvedNamedReferenceImpl(
            name,
            resolvedSymbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedNamedReference(init: AstResolvedNamedReferenceBuilder.() -> Unit): AstResolvedNamedReference {
    return AstResolvedNamedReferenceBuilder().apply(init).build()
}
