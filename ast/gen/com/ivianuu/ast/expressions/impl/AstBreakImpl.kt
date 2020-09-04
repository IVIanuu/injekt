package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.expressions.AstBreak
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstBreakImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var target: AstTarget<AstLoop>,
) : AstBreak() {
    override val type: AstType get() = context.builtIns.nothingType

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstBreakImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {}

    override fun replaceTarget(newTarget: AstTarget<AstLoop>) {
        target = newTarget
    }
}
