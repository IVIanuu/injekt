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
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var exception: AstExpression

    override fun build(): AstThrow {
        return AstThrowImpl(
            annotations,
            type,
            exception,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThrow(init: AstThrowBuilder.() -> Unit): AstThrow {
    return AstThrowBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstThrow.copy(init: AstThrowBuilder.() -> Unit = {}): AstThrow {
    val copyBuilder = AstThrowBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.exception = exception
    return copyBuilder.apply(init).build()
}
