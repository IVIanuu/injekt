package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTryImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var tryBody: AstExpression,
    override val catches: MutableList<AstCatch>,
    override var finallyBody: AstExpression?,
) : AstTry() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        tryBody.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBody?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTryImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        tryBody = tryBody.transformSingle(transformer, data)
        catches.transformInplace(transformer, data)
        finallyBody = finallyBody?.transformSingle(transformer, data)
        return this
    }
}
