package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDoWhileLoop : AstLoop() {
    abstract override val annotations: List<AstCall>
    abstract override val block: AstBlock
    abstract override val condition: AstExpression
    abstract override val label: AstLabel?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDoWhileLoop(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstDoWhileLoop

    abstract override fun <D> transformBlock(transformer: AstTransformer<D>, data: D): AstDoWhileLoop

    abstract override fun <D> transformCondition(transformer: AstTransformer<D>, data: D): AstDoWhileLoop

    abstract override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstDoWhileLoop
}
