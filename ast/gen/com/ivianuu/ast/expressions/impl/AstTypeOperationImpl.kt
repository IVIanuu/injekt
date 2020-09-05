package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.AstTypeOperator
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeOperationImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override var operator: AstTypeOperator,
    override var argument: AstExpression,
    override var typeOperand: AstType,
) : AstTypeOperation() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        argument.accept(visitor, data)
        typeOperand.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeOperationImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        argument = argument.transformSingle(transformer, data)
        typeOperand = typeOperand.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceOperator(newOperator: AstTypeOperator) {
        operator = newOperator
    }

    override fun replaceArgument(newArgument: AstExpression) {
        argument = newArgument
    }

    override fun replaceTypeOperand(newTypeOperand: AstType) {
        typeOperand = newTypeOperand
    }
}
