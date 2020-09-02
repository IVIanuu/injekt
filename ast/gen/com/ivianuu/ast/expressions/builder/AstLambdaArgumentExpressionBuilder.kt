package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstLambdaArgumentExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstLambdaArgumentExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var expression: AstExpression

    override fun build(): AstLambdaArgumentExpression {
        return AstLambdaArgumentExpressionImpl(
            annotations,
            expression,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstLambdaArgumentExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildLambdaArgumentExpression(init: AstLambdaArgumentExpressionBuilder.() -> Unit): AstLambdaArgumentExpression {
    return AstLambdaArgumentExpressionBuilder().apply(init).build()
}
