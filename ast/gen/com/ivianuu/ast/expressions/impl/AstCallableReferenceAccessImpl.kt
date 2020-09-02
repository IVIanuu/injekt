package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstCallableReferenceAccessImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var dispatchReceiver: AstExpression?,
    override var extensionReceiver: AstExpression?,
    override var callee: AstCallableSymbol<*>,
    override var hasQuestionMarkAtLHS: Boolean,
) : AstCallableReferenceAccess() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstCallableReferenceAccessImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstCallableReferenceAccessImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstCallableReferenceAccessImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstCallableReferenceAccessImpl {
        dispatchReceiver = dispatchReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstCallableReferenceAccessImpl {
        extensionReceiver = extensionReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCallee(transformer: AstTransformer<D>, data: D): AstCallableReferenceAccessImpl {
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceCallee(newCallee: AstCallableSymbol<*>) {
        callee = newCallee
    }

    override fun replaceHasQuestionMarkAtLHS(newHasQuestionMarkAtLHS: Boolean) {
        hasQuestionMarkAtLHS = newHasQuestionMarkAtLHS
    }
}
