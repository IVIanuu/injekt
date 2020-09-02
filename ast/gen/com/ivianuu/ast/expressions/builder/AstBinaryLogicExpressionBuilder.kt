package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBinaryLogicExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.LogicOperationKind
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstBinaryLogicExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstBinaryLogicExpressionBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var leftOperand: AstExpression
    lateinit var rightOperand: AstExpression
    lateinit var kind: LogicOperationKind

    override fun build(): AstBinaryLogicExpression {
        return AstBinaryLogicExpressionImpl(
            type,
            annotations,
            leftOperand,
            rightOperand,
            kind,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBinaryLogicExpression(init: AstBinaryLogicExpressionBuilder.() -> Unit): AstBinaryLogicExpression {
    return AstBinaryLogicExpressionBuilder().apply(init).build()
}
