package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTypeOperationImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeOperationBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var operation: AstOperation

    override fun build(): AstTypeOperation {
        return AstTypeOperationImpl(
            type,
            annotations,
            operation,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeOperation(init: AstTypeOperationBuilder.() -> Unit): AstTypeOperation {
    return AstTypeOperationBuilder().apply(init).build()
}
