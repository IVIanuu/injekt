package com.ivianuu.ast.types

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstStarProjection : AstTypeProjection() {
    abstract override val context: AstContext

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitStarProjection(this, data)
}
