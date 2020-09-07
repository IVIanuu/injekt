package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstQualifiedAccess : AstCalleeReference(), AstBaseQualifiedAccess {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val callee: AstSymbol<*>
    abstract override val typeArguments: List<AstType>
    abstract override val dispatchReceiver: AstExpression?
    abstract override val extensionReceiver: AstExpression?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceCallee(newCallee: AstSymbol<*>)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstType>)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: AstExpression?)
}
