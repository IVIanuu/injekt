package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstLoopJump : AstJump<AstLoop>() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract override val target: AstTarget<AstLoop>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitLoopJump(this, data)
}
