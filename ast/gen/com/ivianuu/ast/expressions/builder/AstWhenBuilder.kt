package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstWhenImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstWhenBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    var subject: AstExpression? = null
    var subjectVariable: AstVariable<*>? = null
    val branches: MutableList<AstWhenBranch> = mutableListOf()

    override fun build(): AstWhen {
        return AstWhenImpl(
            type,
            annotations,
            subject,
            subjectVariable,
            branches,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhen(init: AstWhenBuilder.() -> Unit): AstWhen {
    return AstWhenBuilder().apply(init).build()
}
