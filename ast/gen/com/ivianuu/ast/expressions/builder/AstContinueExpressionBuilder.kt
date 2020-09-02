package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.impl.AstContinueExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstContinueExpressionBuilder : AstLoopJumpBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var target: AstTarget<AstLoop>

    override fun build(): AstContinueExpression {
        return AstContinueExpressionImpl(
            type,
            annotations,
            target,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildContinueExpression(init: AstContinueExpressionBuilder.() -> Unit): AstContinueExpression {
    return AstContinueExpressionBuilder().apply(init).build()
}
