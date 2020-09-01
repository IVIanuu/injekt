package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstThrowExpression
import com.ivianuu.ast.expressions.impl.AstThrowExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThrowExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var exception: AstExpression

    override fun build(): AstThrowExpression {
        return AstThrowExpressionImpl(
            annotations,
            exception,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstThrowExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThrowExpression(init: AstThrowExpressionBuilder.() -> Unit): AstThrowExpression {
    return AstThrowExpressionBuilder().apply(init).build()
}
