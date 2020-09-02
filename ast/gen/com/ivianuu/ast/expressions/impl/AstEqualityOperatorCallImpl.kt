package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstEqualityOperatorCallImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val valueArguments: MutableList<AstExpression>,
    override val operation: AstOperation,
) : AstEqualityOperatorCall() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        valueArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEqualityOperatorCallImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        valueArguments.transformInplace(transformer, data)
        return this
    }
}
