package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstClassReferenceImpl(
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override var classType: AstType,
) : AstClassReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        classType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstClassReferenceImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        classType = classType.transformSingle(transformer, data)
        return this
    }
}
