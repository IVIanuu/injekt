package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeProjectionWithVarianceImpl(
    override val context: AstContext,
    override var type: AstType,
    override var variance: Variance,
) : AstTypeProjectionWithVariance() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeProjectionWithVarianceImpl {
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceVariance(newVariance: Variance) {
        variance = newVariance
    }
    override fun equals(other: Any?): Boolean {
        return this === other || (other is AstTypeProjectionWithVariance && type == other.type && variance == other.variance)
    }
    override fun hashCode(): Int {
        var result = type.hashCode()
result += 31 * result + variance.hashCode()
return result
    }
}
