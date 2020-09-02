package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhenSubjectExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val whenRef: AstExpressionRef<AstWhenExpression>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhenSubjectExpression(this, data)
}
