package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBinaryLogicExpression
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.LogicOperationKind
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstBinaryLogicExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var leftOperand: AstExpression,
    override var rightOperand: AstExpression,
    override val kind: LogicOperationKind,
) : AstBinaryLogicExpression() {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        leftOperand.accept(visitor, data)
        rightOperand.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstBinaryLogicExpressionImpl {
        transformLeftOperand(transformer, data)
        transformRightOperand(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstBinaryLogicExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformLeftOperand(
        transformer: AstTransformer<D>,
        data: D
    ): AstBinaryLogicExpressionImpl {
        leftOperand = leftOperand.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRightOperand(
        transformer: AstTransformer<D>,
        data: D
    ): AstBinaryLogicExpressionImpl {
        rightOperand = rightOperand.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstBinaryLogicExpressionImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {
        typeRef = newTypeRef
    }
}
