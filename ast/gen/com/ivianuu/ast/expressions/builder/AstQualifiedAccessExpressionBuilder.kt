package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstQualifiedAccessExpression
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.expressions.impl.AstQualifiedAccessExpressionImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstQualifiedAccessExpressionBuilder : AstQualifiedAccessBuilder,
    AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var calleeReference: AstReference
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var explicitReceiver: AstExpression? = null
    override var dispatchReceiver: AstExpression = AstNoReceiverExpression
    override var extensionReceiver: AstExpression = AstNoReceiverExpression

    override fun build(): AstQualifiedAccessExpression {
        return AstQualifiedAccessExpressionImpl(
            typeRef,
            annotations,
            calleeReference,
            typeArguments,
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildQualifiedAccessExpression(init: AstQualifiedAccessExpressionBuilder.() -> Unit): AstQualifiedAccessExpression {
    return AstQualifiedAccessExpressionBuilder().apply(init).build()
}
