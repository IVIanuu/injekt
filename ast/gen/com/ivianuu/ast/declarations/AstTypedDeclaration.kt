package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypedDeclaration : AstAnnotatedDeclaration {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstFunctionCall>
    val returnType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypedDeclaration(this, data)

    fun replaceReturnType(newReturnType: AstType)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypedDeclaration

    fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstTypedDeclaration
}
