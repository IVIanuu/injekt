package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstTypedDeclaration : AstAnnotatedDeclaration {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    val returnTypeRef: AstTypeRef

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypedDeclaration(this, data)

    fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstTypedDeclaration

    fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstTypedDeclaration
}
