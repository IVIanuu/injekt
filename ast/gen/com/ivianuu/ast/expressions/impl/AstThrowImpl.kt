package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstThrowImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var exception: AstExpression,
) : AstThrow() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        exception.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstThrowImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        exception = exception.transformSingle(transformer, data)
        return this
    }
}
