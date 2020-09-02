package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstGetClassCallImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val valueArguments: MutableList<AstExpression>,
) : AstGetClassCall() {
    override val valueArgument: AstExpression get() = valueArguments.first()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        valueArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstGetClassCallImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        valueArguments.transformInplace(transformer, data)
        return this
    }
}
