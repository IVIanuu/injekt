package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWrappedDelegateExpression
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWrappedDelegateExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var expression: AstExpression,
    override var delegateProvider: AstExpression,
) : AstWrappedDelegateExpression() {
    override val typeRef: AstTypeRef get() = expression.typeRef

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
        delegateProvider.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstWrappedDelegateExpressionImpl {
        transformAnnotations(transformer, data)
        expression = expression.transformSingle(transformer, data)
        delegateProvider = delegateProvider.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstWrappedDelegateExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {}
}
