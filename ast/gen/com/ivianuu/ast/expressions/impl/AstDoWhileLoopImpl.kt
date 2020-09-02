package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDoWhileLoopImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var body: AstExpression,
    override var condition: AstExpression,
    override val label: String?,
) : AstDoWhileLoop() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
        condition.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        annotations.transformInplace(transformer, data)
        body = body.transformSingle(transformer, data)
        condition = condition.transformSingle(transformer, data)
        return this
    }
}
