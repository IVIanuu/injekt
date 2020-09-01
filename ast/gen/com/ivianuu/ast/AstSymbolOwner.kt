package com.ivianuu.ast

import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstSymbolOwner<E> : AstElement where E : AstSymbolOwner<E>, E : AstDeclaration {
    val symbol: AbstractAstSymbol<E>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitSymbolOwner(this, data)
}
