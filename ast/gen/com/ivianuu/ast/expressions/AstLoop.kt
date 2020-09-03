package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstLoop : AstPureAbstractElement(), AstExpression, AstTargetElement {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract val body: AstExpression
    abstract val condition: AstExpression
    abstract val label: String?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitLoop(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceBody(newBody: AstExpression)

    abstract fun replaceCondition(newCondition: AstExpression)

    abstract fun replaceLabel(newLabel: String?)
}
