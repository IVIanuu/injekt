package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstForLoop
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstForLoopImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var label: String?,
    override var body: AstExpression,
    override var loopRange: AstExpression,
    override var loopParameter: AstValueParameter,
) : AstForLoop() {
    override val type: AstType get() = context.builtIns.unitType

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
        loopRange.accept(visitor, data)
        loopParameter.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstForLoopImpl {
        annotations.transformInplace(transformer, data)
        body = body.transformSingle(transformer, data)
        loopRange = loopRange.transformSingle(transformer, data)
        loopParameter = loopParameter.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {}

    override fun replaceLabel(newLabel: String?) {
        label = newLabel
    }

    override fun replaceBody(newBody: AstExpression) {
        body = newBody
    }

    override fun replaceLoopRange(newLoopRange: AstExpression) {
        loopRange = newLoopRange
    }

    override fun replaceLoopParameter(newLoopParameter: AstValueParameter) {
        loopParameter = newLoopParameter
    }
}
