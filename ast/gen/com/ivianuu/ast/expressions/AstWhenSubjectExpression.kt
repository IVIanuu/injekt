package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhenSubjectExpression : AstExpression() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract val whenRef: AstExpressionRef<AstWhenExpression>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitWhenSubjectExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenSubjectExpression
}
