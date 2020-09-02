package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTryExpression : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val tryBlock: AstBlock
    abstract val catches: List<AstCatch>
    abstract val finallyBlock: AstBlock?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTryExpression(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTryExpression
}
