package com.ivianuu.ast.references.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.impl.AstSimpleNamedReference
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSimpleNamedReferenceBuilder {
    lateinit var name: Name
    var candidateSymbol: AbstractAstBasedSymbol<*>? = null

    @OptIn(AstImplementationDetail::class)
    fun build(): AstNamedReference {
        return AstSimpleNamedReference(
            name,
            candidateSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSimpleNamedReference(init: AstSimpleNamedReferenceBuilder.() -> Unit): AstNamedReference {
    return AstSimpleNamedReferenceBuilder().apply(init).build()
}
