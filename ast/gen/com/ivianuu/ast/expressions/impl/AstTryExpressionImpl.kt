package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTryExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTryExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var tryBlock: AstBlock,
    override val catches: MutableList<AstCatch>,
    override var finallyBlock: AstBlock?,
) : AstTryExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        transformTryBlock(transformer, data)
        transformCatches(transformer, data)
        transformFinallyBlock(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTryBlock(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        tryBlock = tryBlock.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCatches(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        catches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformFinallyBlock(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        finallyBlock = finallyBlock?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTryExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
