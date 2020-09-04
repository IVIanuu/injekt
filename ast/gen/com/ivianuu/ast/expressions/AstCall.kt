package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCall : AstCalleeReference() {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val callee: AstFunctionSymbol<*>
    abstract val valueArguments: List<AstExpression?>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCall(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceCallee(newCallee: AstFunctionSymbol<*>)

    abstract override fun replaceCallee(newCallee: AstSymbol<*>)

    abstract fun replaceValueArguments(newValueArguments: List<AstExpression?>)
}
