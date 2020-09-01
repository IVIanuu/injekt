package com.ivianuu.ast.types.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstComposedSuperTypeRef
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstComposedSuperTypeRefImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override val superTypeRefs: MutableList<AstResolvedTypeRef>,
) : AstComposedSuperTypeRef() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstComposedSuperTypeRefImpl {
        transformAnnotations(transformer, data)
        superTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstComposedSuperTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
