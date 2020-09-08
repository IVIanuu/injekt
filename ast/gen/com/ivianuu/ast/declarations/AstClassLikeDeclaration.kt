package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClassLikeDeclaration<F : AstClassLikeDeclaration<F>> : AstDeclaration, AstSymbolOwner<F> {
    override val context: AstContext
    override val annotations: List<AstFunctionCall>
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val symbol: AstClassLikeSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClassLikeDeclaration(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes)
}
