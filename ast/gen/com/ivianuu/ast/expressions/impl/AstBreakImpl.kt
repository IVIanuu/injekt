package com.ivianuu.ast.expressions.impl

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
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var target: AstLoop,
) : AstBreak() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        target.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstBreakImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        target = target.transformSingle(transformer, data)
        return this
    }
}
