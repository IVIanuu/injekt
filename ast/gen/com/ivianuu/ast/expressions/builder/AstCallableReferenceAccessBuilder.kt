package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.impl.AstCallableReferenceAccessImpl
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
class AstCallableReferenceAccessBuilder : AstQualifiedAccessBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var explicitReceiver: AstExpression? = null
    override var dispatchReceiver: AstExpression = AstNoReceiverExpression
    override var extensionReceiver: AstExpression = AstNoReceiverExpression
    lateinit var calleeReference: AstNamedReference
    var hasQuestionMarkAtLHS: Boolean = false

    override fun build(): AstCallableReferenceAccess {
        return AstCallableReferenceAccessImpl(
            typeRef,
            annotations,
            typeArguments,
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            calleeReference,
            hasQuestionMarkAtLHS,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCallableReferenceAccess(init: AstCallableReferenceAccessBuilder.() -> Unit): AstCallableReferenceAccess {
    return AstCallableReferenceAccessBuilder().apply(init).build()
}
