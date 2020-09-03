package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstReturnImpl
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstReturnBuilder : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var type: AstType
    lateinit var result: AstExpression
    lateinit var target: AstFunctionSymbol<*>

    override fun build(): AstReturn {
        return AstReturnImpl(
            annotations,
            type,
            result,
            target,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildReturn(init: AstReturnBuilder.() -> Unit): AstReturn {
    return AstReturnBuilder().apply(init).build()
}
