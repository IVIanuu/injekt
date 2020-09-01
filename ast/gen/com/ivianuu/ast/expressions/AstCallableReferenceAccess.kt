package com.ivianuu.ast.expressions

import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCallableReferenceAccess : AstQualifiedAccessExpression() {
    abstract override val typeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val typeArguments: List<AstTypeProjection>
    abstract override val explicitReceiver: AstExpression?
    abstract override val dispatchReceiver: AstExpression
    abstract override val extensionReceiver: AstExpression
    abstract override val calleeReference: AstNamedReference
    abstract val hasQuestionMarkAtLHS: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitCallableReferenceAccess(this, data)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: AstExpression?)

    abstract fun replaceCalleeReference(newCalleeReference: AstNamedReference)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract fun replaceHasQuestionMarkAtLHS(newHasQuestionMarkAtLHS: Boolean)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableReferenceAccess

    abstract override fun <D> transformTypeArguments(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableReferenceAccess

    abstract override fun <D> transformExplicitReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableReferenceAccess

    abstract override fun <D> transformDispatchReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableReferenceAccess

    abstract override fun <D> transformExtensionReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableReferenceAccess

    abstract override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableReferenceAccess
}
