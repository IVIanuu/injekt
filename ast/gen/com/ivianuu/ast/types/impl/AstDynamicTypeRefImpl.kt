package com.ivianuu.ast.types.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstDynamicTypeRef
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDynamicTypeRefImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val isMarkedNullable: Boolean,
) : AstDynamicTypeRef() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDynamicTypeRefImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstDynamicTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
