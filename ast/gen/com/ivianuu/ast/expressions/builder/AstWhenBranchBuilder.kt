package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
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
class AstWhenBranchBuilder(override val context: AstContext) : AstBuilder {
    lateinit var condition: AstExpression
    lateinit var result: AstExpression

    fun build(): AstWhenBranch {
        return AstWhenBranchImpl(
            context,
            condition,
            result,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildWhenBranch(init: AstWhenBranchBuilder.() -> Unit): AstWhenBranch {
    return AstWhenBranchBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstWhenBranch.copy(init: AstWhenBranchBuilder.() -> Unit = {}): AstWhenBranch {
    val copyBuilder = AstWhenBranchBuilder(context)
    copyBuilder.condition = condition
    copyBuilder.result = result
    return copyBuilder.apply(init).build()
}
