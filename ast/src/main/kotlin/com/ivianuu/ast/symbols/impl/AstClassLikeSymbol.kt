/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstClassLikeDeclaration
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstClassLikeSymbol<D>(
    val classId: ClassId
) : AstClassifierSymbol<D>() where D : AstClassLikeDeclaration<D>, D : AstSymbolOwner<D> {
    abstract override fun toLookupTag(): ConeClassLikeLookupTag
}

sealed class AstClassSymbol<C : AstClass<C>>(classId: ClassId) : AstClassLikeSymbol<C>(classId) {
    private val lookupTag =
        if (classId.isLocal) ConeClassLookupTagWithFixedSymbol(classId, this)
        else ConeClassLikeLookupTagImpl(classId)

    override fun toLookupTag(): ConeClassLikeLookupTag = lookupTag
}

class AstRegularClassSymbol(classId: ClassId) : AstClassSymbol<AstRegularClass>(classId)

val ANONYMOUS_CLASS_ID = ClassId(FqName.ROOT, FqName.topLevel(Name.special("<anonymous>")), true)

class AstAnonymousObjectSymbol : AstClassSymbol<AstAnonymousObject>(ANONYMOUS_CLASS_ID)

class AstTypeAliasSymbol(classId: ClassId) : AstClassLikeSymbol<AstTypeAlias>(classId) {
    override fun toLookupTag() = ConeClassLikeLookupTagImpl(classId)
}
