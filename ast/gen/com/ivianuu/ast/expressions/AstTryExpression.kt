package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTryExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val tryBlock: AstBlock
    abstract val catches: List<AstCatch>
    abstract val finallyBlock: AstBlock?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTryExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformTryBlock(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformCatches(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformFinallyBlock(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTryExpression
}
