package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.impl.AstFunctionCallImpl
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
open class AstFunctionCallBuilder : AstQualifiedAccessBuilder, AstCallBuilder,
    AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var explicitReceiver: AstExpression? = null
    override var dispatchReceiver: AstExpression = AstNoReceiverExpression
    override var extensionReceiver: AstExpression = AstNoReceiverExpression
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    open lateinit var calleeReference: AstNamedReference

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstFunctionCall {
        return AstFunctionCallImpl(
            typeRef,
            annotations,
            typeArguments,
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            argumentList,
            calleeReference,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCall(init: AstFunctionCallBuilder.() -> Unit): AstFunctionCall {
    return AstFunctionCallBuilder().apply(init).build()
}
