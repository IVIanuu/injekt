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
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override var tryBody: AstExpression,
    override val catches: MutableList<AstCatch>,
    override var finallyBody: AstExpression?,
) : AstTry() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        tryBody.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBody?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTryImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        tryBody = tryBody.transformSingle(transformer, data)
        catches.transformInplace(transformer, data)
        finallyBody = finallyBody?.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTryBody(newTryBody: AstExpression) {
        tryBody = newTryBody
    }

    override fun replaceCatches(newCatches: List<AstCatch>) {
        catches.clear()
        catches.addAll(newCatches)
    }

    override fun replaceFinallyBody(newFinallyBody: AstExpression?) {
        finallyBody = newFinallyBody
    }
}
