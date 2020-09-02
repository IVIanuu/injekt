package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstElvisExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var lhs: AstExpression,
    override var rhs: AstExpression,
) : AstElvisExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        lhs.accept(visitor, data)
        rhs.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElvisExpressionImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        lhs = lhs.transformSingle(transformer, data)
        rhs = rhs.transformSingle(transformer, data)
        return this
    }
}
