package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstComponentCall
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstComponentCallImpl
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstComponentCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder,
    AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    var dispatchReceiver: AstExpression = AstNoReceiverExpression
    var extensionReceiver: AstExpression = AstNoReceiverExpression
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    lateinit var explicitReceiver: AstExpression
    var componentIndex: Int by kotlin.properties.Delegates.notNull<Int>()

    override fun build(): AstComponentCall {
        return AstComponentCallImpl(
            annotations,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            argumentList,
            explicitReceiver,
            componentIndex,
        )
    }

    @Deprecated(
        "Modification of 'typeRef' has no impact for AstComponentCallBuilder",
        level = DeprecationLevel.HIDDEN
    )
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildComponentCall(init: AstComponentCallBuilder.() -> Unit): AstComponentCall {
    return AstComponentCallBuilder().apply(init).build()
}
