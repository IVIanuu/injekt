package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThrowExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstThrowExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThrowExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var exception: AstExpression

    override fun build(): AstThrowExpression {
        return AstThrowExpressionImpl(
            type,
            annotations,
            exception,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThrowExpression(init: AstThrowExpressionBuilder.() -> Unit): AstThrowExpression {
    return AstThrowExpressionBuilder().apply(init).build()
}
