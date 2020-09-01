package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstTypeProjectionWithVarianceImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeProjectionWithVarianceBuilder {
    lateinit var typeRef: AstTypeRef
    lateinit var variance: Variance

    fun build(): AstTypeProjectionWithVariance {
        return AstTypeProjectionWithVarianceImpl(
            typeRef,
            variance,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeProjectionWithVariance(init: AstTypeProjectionWithVarianceBuilder.() -> Unit): AstTypeProjectionWithVariance {
    return AstTypeProjectionWithVarianceBuilder().apply(init).build()
}
