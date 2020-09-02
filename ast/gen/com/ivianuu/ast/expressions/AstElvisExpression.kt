package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstElvisExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val lhs: AstExpression
    abstract val rhs: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitElvisExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstElvisExpression

    abstract fun <D> transformLhs(transformer: AstTransformer<D>, data: D): AstElvisExpression

    abstract fun <D> transformRhs(transformer: AstTransformer<D>, data: D): AstElvisExpression
}
