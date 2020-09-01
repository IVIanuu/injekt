package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstDelegateFieldReference
import com.ivianuu.ast.references.impl.AstDelegateFieldReferenceImpl
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstDelegateFieldReferenceBuilder {
    var candidateSymbol: AbstractAstBasedSymbol<*>? = null
    lateinit var resolvedSymbol: AstDelegateFieldSymbol<*>

    fun build(): AstDelegateFieldReference {
        return AstDelegateFieldReferenceImpl(
            candidateSymbol,
            resolvedSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegateFieldReference(init: AstDelegateFieldReferenceBuilder.() -> Unit): AstDelegateFieldReference {
    return AstDelegateFieldReferenceBuilder().apply(init).build()
}
