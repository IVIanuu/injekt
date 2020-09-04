package com.ivianuu.ast.types

import com.ivianuu.ast.AstContext
import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeProjectionWithVariance : AstTypeProjection() {
    abstract override val context: AstContext
    abstract val type: AstType
    abstract val variance: Variance

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeProjectionWithVariance(this, data)

    abstract fun replaceType(newType: AstType)

    abstract fun replaceVariance(newVariance: Variance)
}
