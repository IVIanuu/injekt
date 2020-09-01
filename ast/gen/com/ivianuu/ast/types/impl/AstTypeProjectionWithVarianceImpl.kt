package com.ivianuu.ast.types.impl

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeProjectionWithVarianceImpl(
    override var type: AstType,
    override val variance: Variance,
) : AstTypeProjectionWithVariance() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeProjectionWithVarianceImpl {
        type = type.transformSingle(transformer, data)
        return this
    }
}
