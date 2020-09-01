package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstArgumentList : AstPureAbstractElement(), AstElement {
    abstract val arguments: List<AstExpression>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitArgumentList(this, data)

    abstract fun <D> transformArguments(transformer: AstTransformer<D>, data: D): AstArgumentList
}
