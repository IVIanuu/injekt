package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstBlock : AstExpression() {
    abstract override val annotations: List<AstFunctionCall>
    abstract val statements: List<AstStatement>
    abstract override val type: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstBlock
}
