package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThisReceiverExpression
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstThisReceiverExpressionImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var calleeReference: AstThisReference,
) : AstThisReceiverExpression() {
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstThisReceiverExpressionImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstThisReceiverExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstThisReceiverExpressionImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstThisReceiverExpressionImpl {
        dispatchReceiver = dispatchReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstThisReceiverExpressionImpl {
        extensionReceiver = extensionReceiver?.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }
}
