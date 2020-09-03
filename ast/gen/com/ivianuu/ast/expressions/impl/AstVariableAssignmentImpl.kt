package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstVariableAssignmentImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override val typeArguments: MutableList<AstTypeProjection>,
    override var dispatchReceiver: AstExpression?,
    override var extensionReceiver: AstExpression?,
    override var callee: AstVariableSymbol<*>,
    override var value: AstExpression,
) : AstVariableAssignment() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
        value.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstVariableAssignmentImpl {
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        typeArguments.transformInplace(transformer, data)
        value = value.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: AstExpression?) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceExtensionReceiver(newExtensionReceiver: AstExpression?) {
        extensionReceiver = newExtensionReceiver
    }

    override fun replaceCallee(newCallee: AstVariableSymbol<*>) {
        callee = newCallee
    }

    override fun replaceCallee(newCallee: AstSymbol<*>) {
        require(newCallee is AstVariableSymbol<*>)
        replaceCallee(newCallee)
    }

    override fun replaceValue(newValue: AstExpression) {
        value = newValue
    }
}
