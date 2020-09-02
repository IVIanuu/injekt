package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstBinaryLogicExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val leftOperand: AstExpression
    abstract val rightOperand: AstExpression
    abstract val kind: LogicOperationKind

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitBinaryLogicExpression(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstBinaryLogicExpression
}
