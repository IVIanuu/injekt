package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstExplicitThisReference
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstExplicitThisReferenceBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var labelName: String? = null

    override fun build(): AstThisReference {
        return AstExplicitThisReference(
            type,
            annotations,
            labelName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExplicitThisReference(init: AstExplicitThisReferenceBuilder.() -> Unit): AstThisReference {
    return AstExplicitThisReferenceBuilder().apply(init).build()
}
