package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstBreak : AstLoopJump() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val type: AstType
    abstract override val target: AstLoop

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitBreak(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun replaceTarget(newTarget: AstLoop)
}
