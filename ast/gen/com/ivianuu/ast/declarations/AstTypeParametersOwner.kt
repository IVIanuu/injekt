package com.ivianuu.ast.declarations

import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypeParametersOwner : AstTypeParameterRefsOwner {
    override val typeParameters: List<AstTypeParameter>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeParametersOwner(this, data)

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstTypeParametersOwner
}
