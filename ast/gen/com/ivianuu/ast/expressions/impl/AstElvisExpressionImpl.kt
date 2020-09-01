package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstElvisExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var calleeReference: AstReference,
    override var lhs: AstExpression,
    override var rhs: AstExpression,
) : AstElvisExpression() {
    override var type: AstType = AstImplicitTypeImpl()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        lhs.accept(visitor, data)
        rhs.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElvisExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformCalleeReference(transformer, data)
        transformLhs(transformer, data)
        transformRhs(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstElvisExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: AstTransformer<D>, data: D): AstElvisExpressionImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformLhs(transformer: AstTransformer<D>, data: D): AstElvisExpressionImpl {
        lhs = lhs.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRhs(transformer: AstTransformer<D>, data: D): AstElvisExpressionImpl {
        rhs = rhs.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        calleeReference = newCalleeReference
    }
}
