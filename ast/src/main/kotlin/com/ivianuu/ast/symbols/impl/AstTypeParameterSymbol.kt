/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.name.Name

class AstTypeParameterSymbol : AstClassifierSymbol<AstTypeParameter>() {

    val name: Name
        get() = ast.name

    private val lookupTag = ConeTypeParameterLookupTag(this)

    override fun toLookupTag(): ConeTypeParameterLookupTag = lookupTag
}

