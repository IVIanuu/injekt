package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCall : AstPureAbstractElement(), AstExpression {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val valueArguments: List<AstExpression>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCall(this, data)
}
