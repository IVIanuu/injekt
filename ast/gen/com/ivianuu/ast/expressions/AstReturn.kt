package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstReturn : AstJump<AstFunction<*>>() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val target: AstTarget<AstFunction<*>>
    abstract val result: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitReturn(this, data)
}
