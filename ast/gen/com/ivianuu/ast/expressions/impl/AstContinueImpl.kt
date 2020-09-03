package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstContinue
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstContinueImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override var target: AstLoop,
) : AstContinue() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        target.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstContinueImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        target = target.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTarget(newTarget: AstLoop) {
        target = newTarget
    }
}
