package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAssignmentOperatorStatement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAssignmentOperatorStatementImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val operation: AstOperation,
    override var leftArgument: AstExpression,
    override var rightArgument: AstExpression,
) : AstAssignmentOperatorStatement() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        leftArgument.accept(visitor, data)
        rightArgument.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAssignmentOperatorStatementImpl {
        annotations.transformInplace(transformer, data)
        leftArgument = leftArgument.transformSingle(transformer, data)
        rightArgument = rightArgument.transformSingle(transformer, data)
        return this
    }
}
