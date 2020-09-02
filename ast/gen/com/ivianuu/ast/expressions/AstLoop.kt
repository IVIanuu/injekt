package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstLoop : AstPureAbstractElement(), AstStatement, AstTargetElement {
    abstract override val annotations: List<AstFunctionCall>
    abstract val body: AstExpression
    abstract val condition: AstExpression
    abstract val label: String?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitLoop(this, data)
}
