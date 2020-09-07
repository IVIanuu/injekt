package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCallableReference : AstPureAbstractElement(), AstBaseQualifiedAccess {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val typeArguments: List<AstType>
    abstract override val dispatchReceiver: AstExpression?
    abstract override val extensionReceiver: AstExpression?
    abstract val callee: AstCallableSymbol<*>
    abstract val hasQuestionMarkAtLHS: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableReference(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceTypeArguments(newTypeArguments: List<AstType>)

    abstract override fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?)

    abstract override fun replaceExtensionReceiver(newExtensionReceiver: AstExpression?)

    abstract fun replaceCallee(newCallee: AstCallableSymbol<*>)

    abstract fun replaceHasQuestionMarkAtLHS(newHasQuestionMarkAtLHS: Boolean)
}
