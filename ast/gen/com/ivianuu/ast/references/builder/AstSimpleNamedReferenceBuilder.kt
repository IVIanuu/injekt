package com.ivianuu.ast.references.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.impl.AstSimpleNamedReference
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSimpleNamedReferenceBuilder {
    lateinit var name: Name
    var candidateSymbol: AbstractAstSymbol<*>? = null

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
