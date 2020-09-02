package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCallableMemberDeclaration<F : AstCallableMemberDeclaration<F>> : AstCallableDeclaration<F>, AstMemberDeclaration {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstFunctionCall>
    override val returnType: AstType
    override val receiverType: AstType?
    override val symbol: AstCallableSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableMemberDeclaration(this, data)

    override fun replaceReturnType(newReturnType: AstType)

    override fun replaceReceiverType(newReceiverType: AstType?)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstCallableMemberDeclaration<F>
}
