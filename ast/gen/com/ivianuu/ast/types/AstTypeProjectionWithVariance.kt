package com.ivianuu.ast.types

import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeProjectionWithVariance : AstTypeProjection() {
    abstract val typeRef: AstTypeRef
    abstract val variance: Variance

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeProjectionWithVariance(this, data)
}
