package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstConstExpression
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstConstExpressionImpl<T> (
    override var type: AstType,
    override val annotations: MutableList<AstCall>,
    override var kind: AstConstKind<T>,
    override val value: T,
) : AstConstExpression<T>() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstConstExpressionImpl<T> {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstConstExpressionImpl<T> {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceKind(newKind: AstConstKind<T>) {
        kind = newKind
    }
}
