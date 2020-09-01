package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypeParameterRefsOwner : AstElement {
    val typeParameters: List<AstTypeParameterRef>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeParameterRefsOwner(this, data)

    fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstTypeParameterRefsOwner
}
