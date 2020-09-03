/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.declarations.AstDeclaration

abstract class AbstractAstSymbol<E> :
    AstSymbol<E> where E : AstSymbolOwner<E>, E : AstDeclaration {

    override val isBound: Boolean
        get() = this::owner.isInitialized

    override lateinit var owner: E

    override fun bind(e: E) {
        owner = e
    }

}
