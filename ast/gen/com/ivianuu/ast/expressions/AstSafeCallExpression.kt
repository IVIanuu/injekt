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
    abstract override val annotations: List<AstFunctionCall>
    abstract val receiver: AstExpression
    abstract val checkedSubjectRef: AstExpressionRef<AstCheckedSafeCallSubject>
    abstract val regularQualifiedAccess: AstQualifiedAccess

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSafeCallExpression(this, data)
}
