package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.impl.AstWhenBranchImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhenBranchBuilder {
    lateinit var condition: AstExpression
    lateinit var result: AstExpression

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

@OptIn(ExperimentalContracts::class)
inline fun AstWhenBranch.copy(init: AstWhenBranchBuilder.() -> Unit = {}): AstWhenBranch {
    val copyBuilder = AstWhenBranchBuilder()
    copyBuilder.condition = condition
    copyBuilder.result = result
    return copyBuilder.apply(init).build()
}
