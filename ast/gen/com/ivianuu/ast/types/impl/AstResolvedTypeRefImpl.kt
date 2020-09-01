package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class AstResolvedTypeRefImpl @AstImplementationDetail constructor(
    override val annotations: MutableList<AstAnnotationCall>,
    override val type: ConeKotlinType,
    override var delegatedTypeRef: AstTypeRef?,
    override val isSuspend: Boolean,
) : AstResolvedTypeRef() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        delegatedTypeRef?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstResolvedTypeRefImpl {
        transformAnnotations(transformer, data)
        delegatedTypeRef = delegatedTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstResolvedTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
