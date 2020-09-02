package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.AstWhenSubjectExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhenSubjectExpressionImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val whenRef: AstExpressionRef<AstWhenExpression>,
) : AstWhenSubjectExpression() {
    override val type: AstType get() = whenRef.value.subject!!.type

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstWhenSubjectExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
