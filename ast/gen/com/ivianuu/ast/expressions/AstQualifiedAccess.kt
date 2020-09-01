package com.ivianuu.ast.expressions

import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstQualifiedAccess : AstResolvable, AstStatement {
    override val calleeReference: AstReference
    override val annotations: List<AstAnnotationCall>
    val typeArguments: List<AstTypeProjection>
    val explicitReceiver: AstExpression?
    val dispatchReceiver: AstExpression
    val extensionReceiver: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccess(this, data)

    override fun replaceCalleeReference(newCalleeReference: AstReference)

    fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    fun replaceExplicitReceiver(newExplicitReceiver: AstExpression?)

    override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstQualifiedAccess

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstQualifiedAccess

    fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    fun <D> transformExplicitReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess

    fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstQualifiedAccess
}
