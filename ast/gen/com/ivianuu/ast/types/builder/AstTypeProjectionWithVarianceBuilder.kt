package com.ivianuu.ast.types.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.types.impl.AstTypeProjectionWithVarianceImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeProjectionWithVarianceBuilder(override val context: AstContext) : AstBuilder {
    lateinit var type: AstType
    var variance: Variance = Variance.INVARIANT

    fun build(): AstTypeProjectionWithVariance {
        return AstTypeProjectionWithVarianceImpl(
            context,
            type,
            variance,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildTypeProjectionWithVariance(init: AstTypeProjectionWithVarianceBuilder.() -> Unit): AstTypeProjectionWithVariance {
    return AstTypeProjectionWithVarianceBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTypeProjectionWithVariance.copy(init: AstTypeProjectionWithVarianceBuilder.() -> Unit = {}): AstTypeProjectionWithVariance {
    val copyBuilder = AstTypeProjectionWithVarianceBuilder(context)
    copyBuilder.type = type
    copyBuilder.variance = variance
    return copyBuilder.apply(init).build()
}
