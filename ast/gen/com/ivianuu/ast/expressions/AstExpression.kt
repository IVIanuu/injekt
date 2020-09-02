package com.ivianuu.ast.expressions

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstExpression : AstPureAbstractElement(), AstStatement {
    abstract val type: AstType
    abstract override val annotations: List<AstCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitExpression(this, data)

    abstract fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstExpression
}
