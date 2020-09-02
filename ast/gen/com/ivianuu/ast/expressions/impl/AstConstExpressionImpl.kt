package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstConstExpression
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstConstExpressionImpl<T> (
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val kind: AstConstKind<T>,
    override val value: T,
) : AstConstExpression<T>() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstConstExpressionImpl<T> {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }
}
