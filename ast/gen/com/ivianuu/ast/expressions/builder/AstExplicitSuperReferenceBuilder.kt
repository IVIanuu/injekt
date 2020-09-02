package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstExplicitSuperReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstExplicitSuperReferenceBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var labelName: String? = null
    lateinit var superType: AstType

    override fun build(): AstSuperReference {
        return AstExplicitSuperReference(
            type,
            annotations,
            labelName,
            superType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExplicitSuperReference(init: AstExplicitSuperReferenceBuilder.() -> Unit): AstSuperReference {
    return AstExplicitSuperReferenceBuilder().apply(init).build()
}
