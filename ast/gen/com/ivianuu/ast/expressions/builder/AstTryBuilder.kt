package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTryImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTryBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var tryBody: AstExpression
    val catches: MutableList<AstCatch> = mutableListOf()
    var finallyBody: AstExpression? = null

    override fun build(): AstTry {
        return AstTryImpl(
            type,
            annotations,
            tryBody,
            catches,
            finallyBody,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTry(init: AstTryBuilder.() -> Unit): AstTry {
    return AstTryBuilder().apply(init).build()
}
