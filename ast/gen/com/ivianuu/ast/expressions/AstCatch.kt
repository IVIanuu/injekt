package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstCatch : AstPureAbstractElement(), AstElement {
    abstract override val context: AstContext
    abstract val parameter: AstProperty
    abstract val body: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCatch(this, data)

    abstract fun replaceParameter(newParameter: AstProperty)

    abstract fun replaceBody(newBody: AstExpression)
}
