package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstDoWhileLoop : AstLoop() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val body: AstExpression
    abstract override val condition: AstExpression
    abstract override val label: String?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitDoWhileLoop(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceBody(newBody: AstExpression)

    abstract override fun replaceCondition(newCondition: AstExpression)

    abstract override fun replaceLabel(newLabel: String?)
}
