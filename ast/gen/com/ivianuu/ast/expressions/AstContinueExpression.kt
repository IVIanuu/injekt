package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstTarget
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstContinueExpression : AstLoopJump() {
    abstract override val type: AstType
    abstract override val annotations: List<AstCall>
    abstract override val target: AstTarget<AstLoop>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitContinueExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstContinueExpression
}
