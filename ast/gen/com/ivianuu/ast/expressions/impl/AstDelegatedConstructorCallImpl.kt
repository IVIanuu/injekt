package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDelegatedConstructorCallImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val valueArguments: MutableList<AstExpression>,
    override var constructedType: AstType,
    override var dispatchReceiver: AstExpression?,
    override val isThis: Boolean,
) : AstDelegatedConstructorCall() {
    override val isSuper: Boolean get() = !isThis

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        valueArguments.forEach { it.accept(visitor, data) }
        constructedType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCallImpl {
        annotations.transformInplace(transformer, data)
        valueArguments.transformInplace(transformer, data)
        constructedType = constructedType.transformSingle(transformer, data)
        return this
    }
}
