package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstThisReceiverExpression
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.expressions.impl.AstThisReceiverExpressionImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstThisReceiverExpressionBuilder : AstQualifiedAccessBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
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

    @Deprecated("Modification of 'explicitReceiver' has no impact for AstThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var explicitReceiver: AstExpression?
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'dispatchReceiver' has no impact for AstThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var dispatchReceiver: AstExpression
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'extensionReceiver' has no impact for AstThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var extensionReceiver: AstExpression
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThisReceiverExpression(init: AstThisReceiverExpressionBuilder.() -> Unit): AstThisReceiverExpression {
    return AstThisReceiverExpressionBuilder().apply(init).build()
}
