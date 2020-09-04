package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstCallableReference
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

internal class AstCallableReferenceImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var dispatchReceiver: AstExpression?,
    override var extensionReceiver: AstExpression?,
    override var callee: AstCallableSymbol<*>,
    override var hasQuestionMarkAtLHS: Boolean,
) : AstCallableReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstCallableReferenceImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceExtensionReceiver(newExtensionReceiver: AstExpression?) {
        extensionReceiver = newExtensionReceiver
    }

    override fun replaceCallee(newCallee: AstCallableSymbol<*>) {
        callee = newCallee
    }

    override fun replaceHasQuestionMarkAtLHS(newHasQuestionMarkAtLHS: Boolean) {
        hasQuestionMarkAtLHS = newHasQuestionMarkAtLHS
    }
}
