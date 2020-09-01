package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.references.impl.AstExplicitThisReference
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstExplicitThisReferenceBuilder {
    var labelName: String? = null

    fun build(): AstThisReference {
        return AstExplicitThisReference(
            labelName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExplicitThisReference(init: AstExplicitThisReferenceBuilder.() -> Unit = {}): AstThisReference {
    return AstExplicitThisReferenceBuilder().apply(init).build()
}
