package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCallableMemberDeclaration<F : AstCallableMemberDeclaration<F>> : AstCallableDeclaration<F>, AstMemberDeclaration {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    override val returnTypeRef: AstTypeRef
    override val receiverTypeRef: AstTypeRef?
    override val symbol: AstCallableSymbol<F>
    override val typeParameters: List<AstTypeParameterRef>
    override val status: AstDeclarationStatus

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableMemberDeclaration(this, data)

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>

    override fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>

    override fun <D> transformReceiverTypeRef(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>

    override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>
}
