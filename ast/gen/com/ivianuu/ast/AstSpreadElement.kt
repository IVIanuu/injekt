package com.ivianuu.ast

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstSpreadElement : AstPureAbstractElement(), AstVarargElement {
    abstract override val context: AstContext
    abstract val expression: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSpreadElement(this, data)

    abstract fun replaceExpression(newExpression: AstExpression)
}
