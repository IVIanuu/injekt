package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.expressions.impl.AstLambdaArgumentExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstLambdaArgumentExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var expression: AstExpression

    override fun build(): AstLambdaArgumentExpression {
        return AstLambdaArgumentExpressionImpl(
            annotations,
            expression,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstLambdaArgumentExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildLambdaArgumentExpression(init: AstLambdaArgumentExpressionBuilder.() -> Unit): AstLambdaArgumentExpression {
    return AstLambdaArgumentExpressionBuilder().apply(init).build()
}
