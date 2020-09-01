package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhileLoopImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var label: AstLabel?,
    override var condition: AstExpression,
    override var block: AstBlock,
) : AstWhileLoop() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        label?.accept(visitor, data)
        condition.accept(visitor, data)
        block.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstWhileLoopImpl {
        transformCondition(transformer, data)
        transformBlock(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstWhileLoopImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: AstTransformer<D>, data: D): AstWhileLoopImpl {
        condition = condition.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: AstTransformer<D>, data: D): AstWhileLoopImpl {
        block = block.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstWhileLoopImpl {
        transformAnnotations(transformer, data)
        label = label?.transformSingle(transformer, data)
        return this
    }
}
