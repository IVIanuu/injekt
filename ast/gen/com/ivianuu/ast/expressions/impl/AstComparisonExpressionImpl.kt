package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstComparisonExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstCall>,
    override val operation: AstOperation,
    override var compareToCall: AstFunctionCall,
) : AstComparisonExpression() {
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
