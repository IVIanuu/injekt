package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTypeOperatorCallImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeOperatorCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstCall> = mutableListOf()
    override val arguments: MutableList<AstExpression> = mutableListOf()
    lateinit var operation: AstOperation
    lateinit var conversionType: AstType

    override fun build(): AstTypeOperatorCall {
        return AstTypeOperatorCallImpl(
            type,
            annotations,
            arguments,
            operation,
            conversionType,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperatorCall(init: AstTypeOperatorCallBuilder.() -> Unit): AstTypeOperatorCall {
    return AstTypeOperatorCallBuilder().apply(init).build()
}
