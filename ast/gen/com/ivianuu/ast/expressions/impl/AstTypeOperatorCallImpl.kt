package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeOperatorCallImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val valueArguments: MutableList<AstExpression>,
    override val operation: AstOperation,
    override var conversionType: AstType,
) : AstTypeOperatorCall() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        valueArguments.forEach { it.accept(visitor, data) }
        conversionType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeOperatorCallImpl {
        transformOtherChildren(transformer, data)
        return this
    }
}
