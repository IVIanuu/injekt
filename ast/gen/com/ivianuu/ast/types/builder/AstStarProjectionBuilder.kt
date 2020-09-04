package com.ivianuu.ast.types.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.impl.AstStarProjectionImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstStarProjectionBuilder(override val context: AstContext) : AstBuilder {
    fun build(): AstStarProjection {
        return AstStarProjectionImpl(
            context,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildStarProjection(init: AstStarProjectionBuilder.() -> Unit = {}): AstStarProjection {
    return AstStarProjectionBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstStarProjection.copy(init: AstStarProjectionBuilder.() -> Unit = {}): AstStarProjection {
    val copyBuilder = AstStarProjectionBuilder(context)
    return copyBuilder.apply(init).build()
}
