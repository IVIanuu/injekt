package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstLambdaArgumentExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var expression: AstExpression,
) : AstLambdaArgumentExpression() {
    override val typeRef: AstTypeRef get() = expression.typeRef
    override val isSpread: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstLambdaArgumentExpressionImpl {
        transformAnnotations(transformer, data)
        expression = expression.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstLambdaArgumentExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {}
}
