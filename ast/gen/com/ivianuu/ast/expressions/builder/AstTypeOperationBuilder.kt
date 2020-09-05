package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.AstTypeOperator
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstTypeOperationImpl
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.makeNullable
import com.ivianuu.ast.utils.lazyVar
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstTypeOperationBuilder(override val context: AstContext) : AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var type: AstType by lazyVar { 
                    when (operator) {
                        AstTypeOperator.AS -> typeOperand
                        AstTypeOperator.SAFE_AS -> typeOperand.makeNullable()
                        else -> context.builtIns.booleanType
                    }
                 }
    lateinit var operator: AstTypeOperator
    lateinit var argument: AstExpression
    lateinit var typeOperand: AstType

    override fun build(): AstTypeOperation {
        return AstTypeOperationImpl(
            context,
            annotations,
            type,
            operator,
            argument,
            typeOperand,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun AstBuilder.buildTypeOperation(init: AstTypeOperationBuilder.() -> Unit): AstTypeOperation {
    return AstTypeOperationBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstTypeOperation.copy(init: AstTypeOperationBuilder.() -> Unit = {}): AstTypeOperation {
    val copyBuilder = AstTypeOperationBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.type = type
    copyBuilder.operator = operator
    copyBuilder.argument = argument
    copyBuilder.typeOperand = typeOperand
    return copyBuilder.apply(init).build()
}
