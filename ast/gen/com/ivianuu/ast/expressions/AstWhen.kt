package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstWhen : AstPureAbstractElement(), AstExpression {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val subject: AstExpression?
    abstract val subjectVariable: AstVariable<*>?
    abstract val branches: List<AstWhenBranch>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitWhen(this, data)
}
