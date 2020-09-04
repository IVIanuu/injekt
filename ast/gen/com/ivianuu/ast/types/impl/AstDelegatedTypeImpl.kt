package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstDelegatedType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDelegatedTypeImpl(
    override val context: AstContext,
    override var type: AstType,
    override var expression: AstExpression,
) : AstDelegatedType() {
    override val annotations: List<AstFunctionCall> get() = type.annotations
    override val isMarkedNullable: Boolean get() = type.isMarkedNullable

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDelegatedTypeImpl {
        type = type.transformSingle(transformer, data)
        expression = expression.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {}

    override fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean) {}

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceExpression(newExpression: AstExpression) {
        expression = newExpression
    }
}
