package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.impl.AstEqualityOperatorCallImpl
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstEqualityOperatorCallBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var argumentList: AstArgumentList
    lateinit var operation: AstOperation

    override fun build(): AstEqualityOperatorCall {
        return AstEqualityOperatorCallImpl(
            annotations,
            argumentList,
            operation,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstEqualityOperatorCallBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEqualityOperatorCall(init: AstEqualityOperatorCallBuilder.() -> Unit): AstEqualityOperatorCall {
    return AstEqualityOperatorCallBuilder().apply(init).build()
}
