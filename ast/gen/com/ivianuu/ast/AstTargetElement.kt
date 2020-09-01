package com.ivianuu.ast

import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTargetElement : AstElement {
    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTargetElement(this, data)
}
