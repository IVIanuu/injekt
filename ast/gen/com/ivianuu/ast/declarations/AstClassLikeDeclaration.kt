package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClassLikeDeclaration<F : AstClassLikeDeclaration<F>> : AstAnnotatedDeclaration, AstStatement, AstSymbolOwner<F> {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstFunctionCall>
    override val symbol: AstClassLikeSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClassLikeDeclaration(this, data)
}
