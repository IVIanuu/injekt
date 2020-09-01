package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAssignmentOperatorStatement : AstPureAbstractElement(), AstStatement {
    abstract override val annotations: List<AstAnnotationCall>
    abstract val operation: AstOperation
    abstract val leftArgument: AstExpression
    abstract val rightArgument: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAssignmentOperatorStatement(this, data)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAssignmentOperatorStatement

    abstract fun <D> transformLeftArgument(
        transformer: AstTransformer<D>,
        data: D
    ): AstAssignmentOperatorStatement

    abstract fun <D> transformRightArgument(
        transformer: AstTransformer<D>,
        data: D
    ): AstAssignmentOperatorStatement
}
