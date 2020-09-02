package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDoWhileLoop : AstLoop() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val body: AstExpression
    abstract override val condition: AstExpression
    abstract override val label: AstLabel?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDoWhileLoop(this, data)
}
