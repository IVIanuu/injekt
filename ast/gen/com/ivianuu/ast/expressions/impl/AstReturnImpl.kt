package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstReturnImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override var result: AstExpression,
    override var target: AstFunctionSymbol<*>,
) : AstReturn() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstReturnImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        result = result.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceResult(newResult: AstExpression) {
        result = newResult
    }

    override fun replaceTarget(newTarget: AstFunctionSymbol<*>) {
        target = newTarget
    }
}
