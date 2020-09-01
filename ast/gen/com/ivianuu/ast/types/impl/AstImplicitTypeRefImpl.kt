package com.ivianuu.ast.types.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstImplicitTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstImplicitTypeRefImpl : AstImplicitTypeRef() {
    override val annotations: List<AstAnnotationCall> get() = emptyList()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstImplicitTypeRefImpl {
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstImplicitTypeRefImpl {
        return this
    }
}
