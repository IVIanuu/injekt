package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstMemberDeclaration : AstAnnotatedDeclaration {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitMemberDeclaration(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstMemberDeclaration
}
