package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
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
    override var type: AstType,
    override val valueArguments: MutableList<AstExpression>,
    override var dispatchReceiver: AstExpression?,
    override var kind: AstDelegatedConstructorCallKind,
) : AstDelegatedConstructorCall() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        valueArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDelegatedConstructorCallImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        valueArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceValueArguments(newValueArguments: List<AstExpression>) {
        valueArguments.clear()
        valueArguments.addAll(newValueArguments)
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceKind(newKind: AstDelegatedConstructorCallKind) {
        kind = newKind
    }
}
