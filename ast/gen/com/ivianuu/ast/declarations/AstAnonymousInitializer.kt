package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousInitializer : AstPureAbstractElement(), AstDeclaration, AstSymbolOwner<AstAnonymousInitializer> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract val body: AstBlock?
    abstract override val symbol: AstAnonymousInitializerSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnonymousInitializer(this, data)
}
