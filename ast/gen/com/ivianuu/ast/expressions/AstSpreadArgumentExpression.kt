package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSpreadArgumentExpression : AstWrappedArgumentExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstCall>
    abstract override val expression: AstExpression
    abstract override val isSpread: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSpreadArgumentExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstSpreadArgumentExpression
}
