package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstBinaryLogicExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.LogicOperationKind
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstBinaryLogicExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var leftOperand: AstExpression,
    override var rightOperand: AstExpression,
    override val kind: LogicOperationKind,
) : AstBinaryLogicExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        leftOperand.accept(visitor, data)
        rightOperand.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstBinaryLogicExpressionImpl {
        transformOtherChildren(transformer, data)
        return this
    }
}
