package com.ivianuu.ast.types

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDelegatedType : AstPureAbstractElement(), AstType {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val isMarkedNullable: Boolean
    abstract val type: AstType
    abstract val expression: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDelegatedType(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean)

    abstract fun replaceType(newType: AstType)

    abstract fun replaceExpression(newExpression: AstExpression)
}
