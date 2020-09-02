package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.impl.AstVariableAssignmentImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVariableAssignmentBuilder : AstQualifiedAccessBuilder, AstExpressionBuilder {
    override lateinit var type: AstType
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null
    lateinit var left: AstExpression
    lateinit var right: AstExpression

    override fun build(): AstVariableAssignment {
        return AstVariableAssignmentImpl(
            type,
            annotations,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            left,
            right,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVariableAssignment(init: AstVariableAssignmentBuilder.() -> Unit): AstVariableAssignment {
    return AstVariableAssignmentBuilder().apply(init).build()
}
