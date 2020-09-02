package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBreakExpression
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.impl.AstBreakExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBreakExpressionBuilder : AstLoopJumpBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()
    override lateinit var target: AstTarget<AstLoop>

    override fun build(): AstBreakExpression {
        return AstBreakExpressionImpl(
            type,
            annotations,
            target,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBreakExpression(init: AstBreakExpressionBuilder.() -> Unit): AstBreakExpression {
    return AstBreakExpressionBuilder().apply(init).build()
}
