package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVarargArgumentsExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstVarargArgumentsExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val arguments: MutableList<AstExpression>,
    override var varargElementType: AstType,
) : AstVarargArgumentsExpression() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
        varargElementType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstVarargArgumentsExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        arguments.transformInplace(transformer, data)
        varargElementType = varargElementType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstVarargArgumentsExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
