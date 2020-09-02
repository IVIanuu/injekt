/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.symbols.AbstractAstSymbol
import com.ivianuu.ast.symbols.CallableId

abstract class AstCallableSymbol<D : AstCallableDeclaration<D>> : AbstractAstSymbol<D>() {
    abstract val callableId: CallableId
}
