package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstArgumentListImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstArgumentListBuilder {
    val arguments: MutableList<AstExpression> = mutableListOf()

    fun build(): AstArgumentList {
        return AstArgumentListImpl(
            arguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildArgumentList(init: AstArgumentListBuilder.() -> Unit = {}): AstArgumentList {
    return AstArgumentListBuilder().apply(init).build()
}
