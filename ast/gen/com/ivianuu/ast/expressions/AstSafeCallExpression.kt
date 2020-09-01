package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSafeCallExpression : AstExpression() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract val receiver: AstExpression
    abstract val checkedSubjectRef: AstExpressionRef<AstCheckedSafeCallSubject>
    abstract val regularQualifiedAccess: AstQualifiedAccess

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitSafeCallExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract fun replaceRegularQualifiedAccess(newRegularQualifiedAccess: AstQualifiedAccess)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstSafeCallExpression

    abstract fun <D> transformReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstSafeCallExpression

    abstract fun <D> transformRegularQualifiedAccess(
        transformer: AstTransformer<D>,
        data: D
    ): AstSafeCallExpression
}
