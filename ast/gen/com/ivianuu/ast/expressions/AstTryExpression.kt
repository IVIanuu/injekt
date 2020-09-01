package com.ivianuu.ast.expressions

import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTryExpression : AstExpression(), AstResolvable {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val calleeReference: AstReference
    abstract val tryBlock: AstBlock
    abstract val catches: List<AstCatch>
    abstract val finallyBlock: AstBlock?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTryExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformTryBlock(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformCatches(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformFinallyBlock(transformer: AstTransformer<D>, data: D): AstTryExpression

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTryExpression
}
