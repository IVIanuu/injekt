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
    override var type: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val labelName: String?,
    override var superType: AstType,
) : AstSuperReference() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        superType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstSuperReferenceImpl {
        type = type.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        superType = superType.transformSingle(transformer, data)
        return this
    }
}
