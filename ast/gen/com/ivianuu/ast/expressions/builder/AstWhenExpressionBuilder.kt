package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstWhenExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhenExpressionBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var subject: AstExpression? = null
    var subjectVariable: AstVariable<*>? = null
    val branches: MutableList<AstWhenBranch> = mutableListOf()
    var isExhaustive: Boolean = false

    override fun build(): AstWhenExpression {
        return AstWhenExpressionImpl(
            type,
            annotations,
            subject,
            subjectVariable,
            branches,
            isExhaustive,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenExpression(init: AstWhenExpressionBuilder.() -> Unit): AstWhenExpression {
    return AstWhenExpressionBuilder().apply(init).build()
}
