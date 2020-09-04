package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstBaseQualifiedAccess : AstExpression {
    override val context: AstContext
    override val annotations: List<AstFunctionCall>
    override val type: AstType
    val typeArguments: List<AstTypeProjection>
    val dispatchReceiver: AstExpression?
    val extensionReceiver: AstExpression?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitBaseQualifiedAccess(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceType(newType: AstType)

    fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?)

    fun replaceExtensionReceiver(newExtensionReceiver: AstExpression?)
}
