package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhileLoop : AstLoop() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val label: AstLabel?
    abstract override val condition: AstExpression
    abstract override val block: AstBlock

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhileLoop(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstWhileLoop

    abstract override fun <D> transformCondition(transformer: AstTransformer<D>, data: D): AstWhileLoop

    abstract override fun <D> transformBlock(transformer: AstTransformer<D>, data: D): AstWhileLoop

    abstract override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstWhileLoop
}
