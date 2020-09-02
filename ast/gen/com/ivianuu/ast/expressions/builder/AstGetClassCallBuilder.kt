package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstGetClassCallImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstGetClassCallBuilder : AstCallBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val valueArguments: MutableList<AstExpression> = mutableListOf()

    override fun build(): AstGetClassCall {
        return AstGetClassCallImpl(
            type,
            annotations,
            valueArguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildGetClassCall(init: AstGetClassCallBuilder.() -> Unit): AstGetClassCall {
    return AstGetClassCallBuilder().apply(init).build()
}
