package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.impl.AstContinueExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstContinueExpressionBuilder : AstLoopJumpBuilder, AstAnnotationContainerBuilder,
    AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override lateinit var target: AstTarget<AstLoop>

    override fun build(): AstContinueExpression {
        return AstContinueExpressionImpl(
            annotations,
            target,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstContinueExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildContinueExpression(init: AstContinueExpressionBuilder.() -> Unit): AstContinueExpression {
    return AstContinueExpressionBuilder().apply(init).build()
}
