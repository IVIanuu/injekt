package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDoWhileLoopImpl(
    override val annotations: MutableList<AstCall>,
    override var block: AstBlock,
    override var condition: AstExpression,
    override var label: AstLabel?,
) : AstDoWhileLoop() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        block.accept(visitor, data)
        condition.accept(visitor, data)
        label?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        transformBlock(transformer, data)
        transformCondition(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        block = block.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        condition = condition.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        transformAnnotations(transformer, data)
        label = label?.transformSingle(transformer, data)
        return this
    }
}
