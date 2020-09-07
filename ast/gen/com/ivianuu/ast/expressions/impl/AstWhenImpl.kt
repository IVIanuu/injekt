package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstWhenImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override val branches: MutableList<AstWhenBranch>,
) : AstWhen() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        branches.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstWhenImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        branches.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceBranches(newBranches: List<AstWhenBranch>) {
        branches.clear()
        branches.addAll(newBranches)
    }
}
