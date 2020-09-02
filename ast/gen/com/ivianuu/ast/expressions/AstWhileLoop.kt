package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhileLoop : AstLoop() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val label: String?
    abstract override val condition: AstExpression
    abstract override val body: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhileLoop(this, data)
}
