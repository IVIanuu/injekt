package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstThrowExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitNothingType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstThrowExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var exception: AstExpression,
) : AstThrowExpression() {
    override var type: AstType = AstImplicitNothingType()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        exception.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstThrowExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        exception = exception.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstThrowExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
