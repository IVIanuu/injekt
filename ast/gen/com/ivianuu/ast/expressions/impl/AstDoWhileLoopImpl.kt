package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDoWhileLoopImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var condition: AstExpression,
    override var body: AstExpression,
) : AstDoWhileLoop() {
    override val type: AstType get() = context.builtIns.unitType

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        condition.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDoWhileLoopImpl {
        annotations.transformInplace(transformer, data)
        condition = condition.transformSingle(transformer, data)
        body = body.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {}

    override fun replaceCondition(newCondition: AstExpression) {
        condition = newCondition
    }

    override fun replaceBody(newBody: AstExpression) {
        body = newBody
    }
}
