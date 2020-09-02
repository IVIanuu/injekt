package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeOperatorCall : AstExpression(), AstCall {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val valueArguments: List<AstExpression>
    abstract val operation: AstOperation
    abstract val conversionType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeOperatorCall(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTypeOperatorCall
}
