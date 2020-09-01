package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCallableDeclaration<F : AstCallableDeclaration<F>> : AstTypedDeclaration,
    AstSymbolOwner<F> {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    override val returnTypeRef: AstTypeRef
    val receiverTypeRef: AstTypeRef?
    override val symbol: AstCallableSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitCallableDeclaration(this, data)

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableDeclaration<F>

    override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableDeclaration<F>

    fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstCallableDeclaration<F>
}
