package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstThrowImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThrowBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var exception: AstExpression

    override fun build(): AstThrow {
        return AstThrowImpl(
            type,
            annotations,
            exception,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThrow(init: AstThrowBuilder.() -> Unit): AstThrow {
    return AstThrowBuilder().apply(init).build()
}
