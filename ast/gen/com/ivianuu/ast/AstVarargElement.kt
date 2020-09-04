package com.ivianuu.ast

import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstVarargElement : AstElement {
    override val context: AstContext

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitVarargElement(this, data)
}
