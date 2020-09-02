package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhileLoopImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val label: String?,
    override var condition: AstExpression,
    override var body: AstExpression,
) : AstWhileLoop() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        condition.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstWhileLoopImpl {
        annotations.transformInplace(transformer, data)
        condition = condition.transformSingle(transformer, data)
        body = body.transformSingle(transformer, data)
        return this
    }
}
