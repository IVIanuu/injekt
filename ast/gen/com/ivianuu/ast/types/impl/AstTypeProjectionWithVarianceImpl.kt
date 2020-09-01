package com.ivianuu.ast.types.impl

import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeProjectionWithVarianceImpl(
    override var typeRef: AstTypeRef,
    override val variance: Variance,
) : AstTypeProjectionWithVariance() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeProjectionWithVarianceImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }
}
