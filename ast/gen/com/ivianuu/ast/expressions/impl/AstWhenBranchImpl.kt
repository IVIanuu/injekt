package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhenBranchImpl(
    override var condition: AstExpression,
    override var result: AstBlock,
) : AstWhenBranch() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        condition.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstWhenBranchImpl {
        transformCondition(transformer, data)
        transformResult(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformCondition(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenBranchImpl {
        condition = condition.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformResult(transformer: AstTransformer<D>, data: D): AstWhenBranchImpl {
        result = result.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenBranchImpl {
        return this
    }
}
