package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStringConcatenationCall
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstStringConcatenationCallImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstStringConcatenationCallBuilder : AstCallBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val valueArguments: MutableList<AstExpression> = mutableListOf()
    override lateinit var type: AstType

    override fun build(): AstStringConcatenationCall {
        return AstStringConcatenationCallImpl(
            annotations,
            valueArguments,
            type,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildStringConcatenationCall(init: AstStringConcatenationCallBuilder.() -> Unit): AstStringConcatenationCall {
    return AstStringConcatenationCallBuilder().apply(init).build()
}
