package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCallableDeclaration<F : AstCallableDeclaration<F>> : AstTypedDeclaration, AstSymbolOwner<F> {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    override val returnType: AstType
    val receiverType: AstType?
    override val symbol: AstCallableSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)

    override fun replaceReturnType(newReturnType: AstType)

    fun replaceReceiverType(newReceiverType: AstType?)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstCallableDeclaration<F>

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstCallableDeclaration<F>

    fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstCallableDeclaration<F>
}
