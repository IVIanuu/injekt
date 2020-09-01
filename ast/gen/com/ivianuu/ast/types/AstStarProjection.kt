package com.ivianuu.ast.types

import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstStarProjection : AstTypeProjection() {
    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitStarProjection(this, data)
}
