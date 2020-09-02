package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstExpression
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
class AstEqualityOperatorCallBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()
    val arguments: MutableList<AstExpression> = mutableListOf()
    lateinit var operation: AstOperation

    override fun build(): AstEqualityOperatorCall {
        return AstEqualityOperatorCallImpl(
            type,
            annotations,
            arguments,
            operation,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEqualityOperatorCall(init: AstEqualityOperatorCallBuilder.() -> Unit): AstEqualityOperatorCall {
    return AstEqualityOperatorCallBuilder().apply(init).build()
}
