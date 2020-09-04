package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstReturnImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var target: AstTarget<AstFunction<*>>,
    override var result: AstExpression,
) : AstReturn() {
    override val type: AstType get() = context.builtIns.nothingType

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstReturnImpl {
        annotations.transformInplace(transformer, data)
        result = result.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {}

    override fun replaceTarget(newTarget: AstTarget<AstFunction<*>>) {
        target = newTarget
    }

    override fun replaceResult(newResult: AstExpression) {
        result = newResult
    }
}
