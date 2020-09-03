package com.ivianuu.ast.impl

import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstSpreadElementImpl(
    override var expression: AstExpression,
) : AstSpreadElement() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSpreadElementImpl {
        expression = expression.transformSingle(transformer, data)
        return this
    }

    override fun replaceExpression(newExpression: AstExpression) {
        expression = newExpression
    }
}
