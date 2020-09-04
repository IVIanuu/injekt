package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhenBranch : AstPureAbstractElement(), AstElement {
    abstract override val context: AstContext
    abstract val condition: AstExpression
    abstract val result: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhenBranch(this, data)

    abstract fun replaceCondition(newCondition: AstExpression)

    abstract fun replaceResult(newResult: AstExpression)
}
