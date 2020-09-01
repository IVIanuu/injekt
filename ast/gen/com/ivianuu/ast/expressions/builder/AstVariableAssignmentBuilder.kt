package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.expressions.impl.AstVariableAssignmentImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVariableAssignmentBuilder : AstQualifiedAccessBuilder, AstAnnotationContainerBuilder {
    lateinit var calleeReference: AstReference
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var explicitReceiver: AstExpression? = null
    override var dispatchReceiver: AstExpression = AstNoReceiverExpression
    override var extensionReceiver: AstExpression = AstNoReceiverExpression
    lateinit var rValue: AstExpression

    override fun build(): AstVariableAssignment {
        return AstVariableAssignmentImpl(
            calleeReference,
            annotations,
            typeArguments,
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            rValue,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVariableAssignment(init: AstVariableAssignmentBuilder.() -> Unit): AstVariableAssignment {
    return AstVariableAssignmentBuilder().apply(init).build()
}
