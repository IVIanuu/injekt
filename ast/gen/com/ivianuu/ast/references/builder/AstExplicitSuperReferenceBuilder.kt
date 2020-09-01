package com.ivianuu.ast.references.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.references.AstSuperReference
import com.ivianuu.ast.references.impl.AstExplicitSuperReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstExplicitSuperReferenceBuilder {
    var labelName: String? = null
    lateinit var superTypeRef: AstTypeRef

    fun build(): AstSuperReference {
        return AstExplicitSuperReference(
            labelName,
            superTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExplicitSuperReference(init: AstExplicitSuperReferenceBuilder.() -> Unit): AstSuperReference {
    return AstExplicitSuperReferenceBuilder().apply(init).build()
}
