package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.references.impl.AstImplicitThisReference
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstImplicitThisReferenceBuilder {
    var boundSymbol: AbstractAstSymbol<*>? = null

    fun build(): AstThisReference {
        return AstImplicitThisReference(
            boundSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitThisReference(init: AstImplicitThisReferenceBuilder.() -> Unit = {}): AstThisReference {
    return AstImplicitThisReferenceBuilder().apply(init).build()
}
