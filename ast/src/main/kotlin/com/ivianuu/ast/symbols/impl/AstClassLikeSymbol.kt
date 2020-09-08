/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstClassLikeDeclaration
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeAlias
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstClassLikeSymbol<D> : AstClassifierSymbol<D>() where D : AstClassLikeDeclaration<D>, D : AstSymbolOwner<D>

sealed class AstClassSymbol<C : AstClass<C>> : AstClassLikeSymbol<C>()

class AstRegularClassSymbol(
    override val fqName: FqName
) : AstClassSymbol<AstRegularClass>() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AstRegularClassSymbol

        if (fqName != other.fqName) return false

        return true
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }
}

val ANONYMOUS_CLASS_ID = Name.special("<anonymous>")

class AstAnonymousObjectSymbol : AstClassSymbol<AstAnonymousObject>() {
    override val fqName: FqName = FqName(ANONYMOUS_CLASS_ID.asString())
}

class AstTypeAliasSymbol(override val fqName: FqName) : AstClassLikeSymbol<AstTypeAlias>()

class AstEnumEntrySymbol(override val fqName: FqName) : AstClassSymbol<AstEnumEntry>()
