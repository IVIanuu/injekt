package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstVariableAssignment : AstPureAbstractElement(), AstQualifiedAccess {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeArguments: List<AstTypeProjection>
    abstract override val dispatchReceiver: AstExpression?
    abstract override val extensionReceiver: AstExpression?
    abstract val left: AstExpression
    abstract val right: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitVariableAssignment(this, data)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstVariableAssignment

    abstract override fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstVariableAssignment

    abstract override fun <D> transformDispatchReceiver(transformer: AstTransformer<D>, data: D): AstVariableAssignment

    abstract override fun <D> transformExtensionReceiver(transformer: AstTransformer<D>, data: D): AstVariableAssignment

    abstract fun <D> transformRight(transformer: AstTransformer<D>, data: D): AstVariableAssignment
}
