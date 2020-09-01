package com.ivianuu.ast.symbols

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.declarations.AstDeclaration

interface PossiblyAstFakeOverrideSymbol<E, S : AstBasedSymbol<E>> :
    AstBasedSymbol<E> where E : AstSymbolOwner<E>, E : AstDeclaration {
    // contract isFakeOverride == true <=> overriddenSymbol != null
    val isFakeOverride: Boolean
    val overriddenSymbol: S?
}