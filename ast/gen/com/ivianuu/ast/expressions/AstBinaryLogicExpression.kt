package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstBinaryLogicExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstCall>
    abstract val leftOperand: AstExpression
    abstract val rightOperand: AstExpression
    abstract val kind: LogicOperationKind

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitBinaryLogicExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstBinaryLogicExpression

    abstract fun <D> transformLeftOperand(transformer: AstTransformer<D>, data: D): AstBinaryLogicExpression

    abstract fun <D> transformRightOperand(transformer: AstTransformer<D>, data: D): AstBinaryLogicExpression

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstBinaryLogicExpression
}
