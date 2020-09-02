package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhenBranchImpl(
    override var condition: AstExpression,
    override var result: AstExpression,
) : AstWhenBranch() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstWhenBranchImpl {
        condition = condition.transformSingle(transformer, data)
        result = result.transformSingle(transformer, data)
        return this
    }
}
