package com.ivianuu.ast.expressions

import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstFunctionCall : AstBaseQualifiedAccess, AstCall() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val typeArguments: List<AstTypeProjection>
    abstract override val dispatchReceiver: AstExpression?
    abstract override val extensionReceiver: AstExpression?
    abstract override val valueArguments: List<AstExpression?>
    abstract override val callee: AstFunctionSymbol<*>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitFunctionCall(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: AstExpression?)

    abstract override fun replaceValueArguments(newValueArguments: List<AstExpression?>)

    abstract fun replaceCallee(newCallee: AstFunctionSymbol<*>)

    abstract override fun replaceCallee(newCallee: AstSymbol<*>)
}
