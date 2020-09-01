package com.ivianuu.ast

import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstLabel : AstPureAbstractElement(), AstElement {
    abstract val name: String

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitLabel(this, data)
}
