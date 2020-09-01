/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.symbols.ConeClassifierLookupTag

abstract class AstClassifierSymbol<E> : AbstractAstBasedSymbol<E>()
        where E : AstSymbolOwner<E>, E : AstDeclaration {
    abstract fun toLookupTag(): ConeClassifierLookupTag
}
