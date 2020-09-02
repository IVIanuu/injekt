package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstNamedArgumentExpression : AstWrappedArgumentExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstCall>
    abstract override val expression: AstExpression
    abstract override val isSpread: Boolean
    abstract val name: Name

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitNamedArgumentExpression(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstNamedArgumentExpression
}
