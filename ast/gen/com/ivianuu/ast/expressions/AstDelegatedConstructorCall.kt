package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDelegatedConstructorCall : AstCall() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val valueArguments: List<AstExpression>
    abstract val dispatchReceiver: AstExpression?
    abstract val kind: AstDelegatedConstructorCallKind

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDelegatedConstructorCall(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceValueArguments(newValueArguments: List<AstExpression>)

    abstract fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?)

    abstract fun replaceKind(newKind: AstDelegatedConstructorCallKind)
}
