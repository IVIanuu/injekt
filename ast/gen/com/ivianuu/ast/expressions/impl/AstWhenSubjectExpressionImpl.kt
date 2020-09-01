package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstExpressionRef
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.AstWhenSubjectExpression
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhenSubjectExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val whenRef: AstExpressionRef<AstWhenExpression>,
) : AstWhenSubjectExpression() {
    override val typeRef: AstTypeRef get() = whenRef.value.subject!!.typeRef

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenSubjectExpressionImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstWhenSubjectExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {}
}
