package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhenBranch : AstPureAbstractElement(), AstElement {
    abstract val condition: AstExpression
    abstract val result: AstBlock

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhenBranch(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstWhenBranch
}
