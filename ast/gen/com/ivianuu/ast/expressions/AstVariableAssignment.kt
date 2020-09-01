package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstVariableAssignment : AstPureAbstractElement(), AstQualifiedAccess {
    abstract override val calleeReference: AstReference
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val typeArguments: List<AstTypeProjection>
    abstract override val explicitReceiver: AstExpression?
    abstract override val dispatchReceiver: AstExpression
    abstract override val extensionReceiver: AstExpression
    abstract val lValue: AstReference
    abstract val rValue: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitVariableAssignment(this, data)

    abstract override fun replaceCalleeReference(newCalleeReference: AstReference)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: AstExpression?)

    abstract override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariableAssignment

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariableAssignment

    abstract override fun <D> transformTypeArguments(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariableAssignment

    abstract override fun <D> transformExplicitReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariableAssignment

    abstract override fun <D> transformDispatchReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariableAssignment

    abstract override fun <D> transformExtensionReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariableAssignment

    abstract fun <D> transformRValue(transformer: AstTransformer<D>, data: D): AstVariableAssignment
}
