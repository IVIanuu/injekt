package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstThisReceiverExpression
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.impl.AstThisReceiverExpressionImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThisReceiverExpressionBuilder : AstQualifiedAccessBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    lateinit var calleeReference: AstThisReference

    override fun build(): AstThisReceiverExpression {
        return AstThisReceiverExpressionImpl(
            type,
            annotations,
            typeArguments,
            calleeReference,
        )
    }

    @Deprecated("Modification of 'dispatchReceiver' has no impact for AstThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var dispatchReceiver: AstExpression?
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'extensionReceiver' has no impact for AstThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var extensionReceiver: AstExpression?
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThisReceiverExpression(init: AstThisReceiverExpressionBuilder.() -> Unit): AstThisReceiverExpression {
    return AstThisReceiverExpressionBuilder().apply(init).build()
}
