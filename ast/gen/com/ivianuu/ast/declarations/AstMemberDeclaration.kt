package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstMemberDeclaration : AstAnnotatedDeclaration, AstTypeParameterRefsOwner {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstCall>
    override val typeParameters: List<AstTypeParameterRef>
    val status: AstDeclarationStatus

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitMemberDeclaration(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstMemberDeclaration

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstMemberDeclaration

    fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstMemberDeclaration
}
