package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstExpressionWithSmartcast : AstQualifiedAccess() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeArguments: List<AstTypeProjection>
    abstract override val dispatchReceiver: AstExpression?
    abstract override val extensionReceiver: AstExpression?
    abstract val originalExpression: AstQualifiedAccess
    abstract val typesFromSmartCast: Collection<AstType>
    abstract val originalType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitExpressionWithSmartcast(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstExpressionWithSmartcast

    abstract override fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstExpressionWithSmartcast

    abstract override fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstExpressionWithSmartcast

    abstract override fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstExpressionWithSmartcast
}
