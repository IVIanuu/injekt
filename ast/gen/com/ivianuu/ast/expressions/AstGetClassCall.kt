package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstGetClassCall : AstExpression(), AstCall {
    abstract override val type: AstType
    abstract override val annotations: List<AstCall>
    abstract override val arguments: List<AstExpression>
    abstract val argument: AstExpression

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitGetClassCall(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstGetClassCall
}
