package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstSpreadArgumentExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstSpreadArgumentExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstSpreadArgumentExpressionBuilder : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var expression: AstExpression

    override fun build(): AstSpreadArgumentExpression {
        return AstSpreadArgumentExpressionImpl(
            annotations,
            expression,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstSpreadArgumentExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildSpreadArgumentExpression(init: AstSpreadArgumentExpressionBuilder.() -> Unit): AstSpreadArgumentExpression {
    return AstSpreadArgumentExpressionBuilder().apply(init).build()
}
