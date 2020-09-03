package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstVarargImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override val elements: MutableList<AstVarargElement>,
) : AstVararg() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        elements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstVarargImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        elements.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceElements(newElements: List<AstVarargElement>) {
        elements.clear()
        elements.addAll(newElements)
    }
}
