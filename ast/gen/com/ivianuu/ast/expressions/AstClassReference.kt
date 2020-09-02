package com.ivianuu.ast.expressions

import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstClassReference : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val classType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClassReference(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstClassReference
}
