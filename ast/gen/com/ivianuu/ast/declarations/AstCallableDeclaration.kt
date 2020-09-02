package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstCallableDeclaration<F : AstCallableDeclaration<F>> : AstDeclaration, AstSymbolOwner<F> {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    val receiverType: AstType?
    val returnType: AstType
    override val symbol: AstCallableSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)
}
