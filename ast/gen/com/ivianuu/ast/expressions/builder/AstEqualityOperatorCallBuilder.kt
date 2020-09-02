package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstEqualityOperatorCallImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstEqualityOperatorCallBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    val valueArguments: MutableList<AstExpression> = mutableListOf()
    lateinit var operation: AstOperation

    override fun build(): AstEqualityOperatorCall {
        return AstEqualityOperatorCallImpl(
            type,
            annotations,
            valueArguments,
            operation,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEqualityOperatorCall(init: AstEqualityOperatorCallBuilder.() -> Unit): AstEqualityOperatorCall {
    return AstEqualityOperatorCallBuilder().apply(init).build()
}
