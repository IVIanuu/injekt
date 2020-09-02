package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstQualifiedAccess : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val typeArguments: List<AstTypeProjection>
    abstract val dispatchReceiver: AstExpression?
    abstract val extensionReceiver: AstExpression?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    abstract fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    abstract fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    abstract fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess
}
