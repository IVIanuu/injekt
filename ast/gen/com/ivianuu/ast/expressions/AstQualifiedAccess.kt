package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstQualifiedAccess : AstStatement {
    override val annotations: List<AstFunctionCall>
    val typeArguments: List<AstTypeProjection>
    val dispatchReceiver: AstExpression?
    val extensionReceiver: AstExpression?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)

    fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess
}
