package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstComparisonExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstComparisonExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var operation: AstOperation
    lateinit var compareToCall: AstFunctionCall

    override fun build(): AstComparisonExpression {
        return AstComparisonExpressionImpl(
            type,
            annotations,
            operation,
            compareToCall,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildComparisonExpression(init: AstComparisonExpressionBuilder.() -> Unit): AstComparisonExpression {
    return AstComparisonExpressionBuilder().apply(init).build()
}
