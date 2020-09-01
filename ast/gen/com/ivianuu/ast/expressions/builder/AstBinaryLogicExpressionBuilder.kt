package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBinaryLogicExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.LogicOperationKind
import com.ivianuu.ast.expressions.impl.AstBinaryLogicExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBinaryLogicExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var leftOperand: AstExpression
    lateinit var rightOperand: AstExpression
    lateinit var kind: LogicOperationKind

    override fun build(): AstBinaryLogicExpression {
        return AstBinaryLogicExpressionImpl(
            annotations,
            leftOperand,
            rightOperand,
            kind,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstBinaryLogicExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryLogicExpression(init: AstBinaryLogicExpressionBuilder.() -> Unit): AstBinaryLogicExpression {
    return AstBinaryLogicExpressionBuilder().apply(init).build()
}
