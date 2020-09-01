package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstAssignmentOperatorStatement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.impl.AstAssignmentOperatorStatementImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAssignmentOperatorStatementBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var operation: AstOperation
    lateinit var leftArgument: AstExpression
    lateinit var rightArgument: AstExpression

    override fun build(): AstAssignmentOperatorStatement {
        return AstAssignmentOperatorStatementImpl(
            annotations,
            operation,
            leftArgument,
            rightArgument,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAssignmentOperatorStatement(init: AstAssignmentOperatorStatementBuilder.() -> Unit): AstAssignmentOperatorStatement {
    return AstAssignmentOperatorStatementBuilder().apply(init).build()
}
