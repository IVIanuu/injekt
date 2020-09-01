package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.impl.AstComparisonExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstComparisonExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var operation: AstOperation
    lateinit var compareToCall: AstFunctionCall

    override fun build(): AstComparisonExpression {
        return AstComparisonExpressionImpl(
            annotations,
            operation,
            compareToCall,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstComparisonExpressionBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildComparisonExpression(init: AstComparisonExpressionBuilder.() -> Unit): AstComparisonExpression {
    return AstComparisonExpressionBuilder().apply(init).build()
}
