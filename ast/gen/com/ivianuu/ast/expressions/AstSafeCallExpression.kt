package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSafeCallExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstAnnotationCall>
    abstract val receiver: AstExpression
    abstract val checkedSubjectRef: AstExpressionRef<AstCheckedSafeCallSubject>
    abstract val regularQualifiedAccess: AstQualifiedAccess

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSafeCallExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceRegularQualifiedAccess(newRegularQualifiedAccess: AstQualifiedAccess)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstSafeCallExpression

    abstract fun <D> transformReceiver(transformer: AstTransformer<D>, data: D): AstSafeCallExpression

    abstract fun <D> transformRegularQualifiedAccess(transformer: AstTransformer<D>, data: D): AstSafeCallExpression
}
