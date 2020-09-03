package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstThisReferenceImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override var labelName: String?,
) : AstThisReference() {
    override var boundSymbol: AstSymbol<*>? = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstThisReferenceImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceLabelName(newLabelName: String?) {
        labelName = newLabelName
    }

    override fun replaceBoundSymbol(newBoundSymbol: AstSymbol<*>?) {
        boundSymbol = newBoundSymbol
    }
}
