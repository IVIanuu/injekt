package com.ivianuu.ast.expressions

import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstElvisExpression : AstExpression(), AstResolvable {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val calleeReference: AstReference
    abstract val lhs: AstExpression
    abstract val rhs: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitElvisExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstElvisExpression

    abstract override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstElvisExpression

    abstract fun <D> transformLhs(transformer: AstTransformer<D>, data: D): AstElvisExpression

    abstract fun <D> transformRhs(transformer: AstTransformer<D>, data: D): AstElvisExpression
}
