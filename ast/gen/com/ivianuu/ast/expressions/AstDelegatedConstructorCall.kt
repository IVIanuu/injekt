package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDelegatedConstructorCall : AstCall() {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val valueArguments: List<AstExpression?>
    abstract override val callee: AstConstructorSymbol
    abstract val dispatchReceiver: AstExpression?
    abstract val kind: AstDelegatedConstructorCallKind

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDelegatedConstructorCall(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceValueArguments(newValueArguments: List<AstExpression?>)

    abstract fun replaceCallee(newCallee: AstConstructorSymbol)

    abstract override fun replaceCallee(newCallee: AstSymbol<*>)

    abstract override fun replaceCallee(newCallee: AstFunctionSymbol<*>)

    abstract fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?)

    abstract fun replaceKind(newKind: AstDelegatedConstructorCallKind)
}
