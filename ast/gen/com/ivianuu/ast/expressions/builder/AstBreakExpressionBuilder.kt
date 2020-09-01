package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBreakExpression
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.impl.AstBreakExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBreakExpressionBuilder : AstLoopJumpBuilder, AstAnnotationContainerBuilder,
    AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override lateinit var target: AstTarget<AstLoop>

    override fun build(): AstBreakExpression {
        return AstBreakExpressionImpl(
            annotations,
            target,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstBreakExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBreakExpression(init: AstBreakExpressionBuilder.() -> Unit): AstBreakExpression {
    return AstBreakExpressionBuilder().apply(init).build()
}
