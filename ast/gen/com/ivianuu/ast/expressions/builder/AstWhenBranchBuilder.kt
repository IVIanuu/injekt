package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.impl.AstWhenBranchImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhenBranchBuilder {
    lateinit var condition: AstExpression
    lateinit var result: AstBlock

    fun build(): AstWhenBranch {
        return AstWhenBranchImpl(
            condition,
            result,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenBranch(init: AstWhenBranchBuilder.() -> Unit): AstWhenBranch {
    return AstWhenBranchBuilder().apply(init).build()
}
