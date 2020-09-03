package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstSuperReferenceImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override var labelName: String?,
    override var superType: AstType,
) : AstSuperReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        superType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSuperReferenceImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        superType = superType.transformSingle(transformer, data)
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

    override fun replaceSuperType(newSuperType: AstType) {
        superType = newSuperType
    }
}
