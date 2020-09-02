package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstReturnExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstReturnExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstReturnExpressionBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var target: AstTarget<AstFunction<*>>
    lateinit var result: AstExpression

    override fun build(): AstReturnExpression {
        return AstReturnExpressionImpl(
            type,
            annotations,
            target,
            result,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildReturnExpression(init: AstReturnExpressionBuilder.() -> Unit): AstReturnExpression {
    return AstReturnExpressionBuilder().apply(init).build()
}
