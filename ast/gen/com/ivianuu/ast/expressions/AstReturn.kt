package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstReturn : AstPureAbstractElement(), AstExpression {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract val result: AstExpression
    abstract val target: AstFunctionSymbol<*>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitReturn(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceResult(newResult: AstExpression)

    abstract fun replaceTarget(newTarget: AstFunctionSymbol<*>)
}
