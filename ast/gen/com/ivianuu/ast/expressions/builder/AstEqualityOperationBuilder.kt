package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstEqualityOperation
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstEqualityOperationImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstEqualityOperationBuilder : AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    lateinit var operation: AstOperation

    override fun build(): AstEqualityOperation {
        return AstEqualityOperationImpl(
            type,
            annotations,
            operation,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEqualityOperation(init: AstEqualityOperationBuilder.() -> Unit): AstEqualityOperation {
    return AstEqualityOperationBuilder().apply(init).build()
}
