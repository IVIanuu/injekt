/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.symbols.AbstractAstBasedSymbol
import com.ivianuu.ast.symbols.CallableId

abstract class AstCallableSymbol<D : AstCallableDeclaration<D>> : AbstractAstBasedSymbol<D>() {
    abstract val callableId: CallableId

    open val overriddenSymbol: AstCallableSymbol<D>?
        get() = null

    open val isIntersectionOverride: Boolean get() = false
}

val AstCallableSymbol<*>.isStatic: Boolean get() = (ast as? AstMemberDeclaration)?.status?.isStatic == true

inline fun <reified E : AstCallableSymbol<*>> E.unwrapSubstitutionOverrides(): E {
    var current = this
    while (current.overriddenSymbol != null) {
        current = current.overriddenSymbol as E
    }

    return current
}
