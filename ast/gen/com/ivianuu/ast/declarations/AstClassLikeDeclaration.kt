package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClassLikeDeclaration<F : AstClassLikeDeclaration<F>> : AstAnnotatedDeclaration, AstStatement, AstSymbolOwner<F> {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val annotations: List<AstAnnotationCall>
    override val symbol: AstClassLikeSymbol<F>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClassLikeDeclaration(this, data)

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstClassLikeDeclaration<F>
}
