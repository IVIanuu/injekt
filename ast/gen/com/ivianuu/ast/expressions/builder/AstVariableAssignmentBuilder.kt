package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstVariableAssignmentImpl
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstVariableAssignmentBuilder(override val context: AstContext) : AstBaseQualifiedAccessBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val typeArguments: MutableList<AstType> = mutableListOf()
    override var dispatchReceiver: AstExpression? = null
    override var extensionReceiver: AstExpression? = null
    lateinit var callee: AstVariableSymbol<*>
    lateinit var value: AstExpression

    override fun build(): AstVariableAssignment {
        return AstVariableAssignmentImpl(
            context,
            annotations,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            callee,
            value,
        )
    }


    @Deprecated("Modification of 'type' has no impact for AstVariableAssignmentBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildVariableAssignment(init: AstVariableAssignmentBuilder.() -> Unit): AstVariableAssignment {
    return AstVariableAssignmentBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstVariableAssignment.copy(init: AstVariableAssignmentBuilder.() -> Unit = {}): AstVariableAssignment {
    val copyBuilder = AstVariableAssignmentBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.typeArguments.addAll(typeArguments)
    copyBuilder.dispatchReceiver = dispatchReceiver
    copyBuilder.extensionReceiver = extensionReceiver
    copyBuilder.callee = callee
    copyBuilder.value = value
    return copyBuilder.apply(init).build()
}
