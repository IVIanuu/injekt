package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitBooleanType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstComparisonExpressionImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val operation: AstOperation,
    override var compareToCall: AstFunctionCall,
) : AstComparisonExpression() {
    override var type: AstType = AstImplicitBooleanType()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        compareToCall.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstComparisonExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        compareToCall = compareToCall.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstComparisonExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
