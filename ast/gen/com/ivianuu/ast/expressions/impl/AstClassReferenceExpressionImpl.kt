package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstClassReferenceExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstClassReferenceExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var classType: AstType,
) : AstClassReferenceExpression() {
    override var type: AstType = AstImplicitTypeImpl()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        classType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstClassReferenceExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        classType = classType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstClassReferenceExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
