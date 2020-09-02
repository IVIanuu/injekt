/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations

import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.declarations.builder.AstRegularClassBuilder
import com.ivianuu.ast.declarations.impl.AstFileImpl
import com.ivianuu.ast.declarations.impl.AstRegularClassImpl
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.ClassId

inline val AstRegularClass.isInner get() = status.isInner
inline val AstRegularClass.isCompanion get() = status.isCompanion
inline val AstRegularClass.isData get() = status.isData
inline val AstRegularClass.isInline get() = status.isInline
inline val AstRegularClass.isFun get() = status.isFun
inline val AstMemberDeclaration.modality get() = status.modality
inline val AstMemberDeclaration.visibility get() = status.visibility
inline val AstMemberDeclaration.isActual get() = status.isActual
inline val AstMemberDeclaration.isExpect get() = status.isExpect
inline val AstMemberDeclaration.isInner get() = status.isInner
inline val AstMemberDeclaration.isStatic get() = status.isStatic
inline val AstMemberDeclaration.isOverride: Boolean get() = status.isOverride
inline val AstMemberDeclaration.isOperator: Boolean get() = status.isOperator
inline val AstMemberDeclaration.isInfix: Boolean get() = status.isInfix
inline val AstMemberDeclaration.isInline: Boolean get() = status.isInline
inline val AstMemberDeclaration.isTailRec: Boolean get() = status.isTailRec
inline val AstMemberDeclaration.isExternal: Boolean get() = status.isExternal
inline val AstMemberDeclaration.isSuspend: Boolean get() = status.isSuspend
inline val AstMemberDeclaration.isConst: Boolean get() = status.isConst
inline val AstMemberDeclaration.isLateInit: Boolean get() = status.isLateInit
inline val AstMemberDeclaration.isFromSealedClass: Boolean get() = status.isFromSealedClass
inline val AstMemberDeclaration.isFromEnumClass: Boolean get() = status.isFromEnumClass

inline val AstPropertyAccessor.modality get() = status.modality
inline val AstPropertyAccessor.visibility get() = status.visibility
inline val AstPropertyAccessor.isInline get() = status.isInline
inline val AstPropertyAccessor.isExternal get() = status.isExternal

inline val AstRegularClass.isLocal get() = symbol.classId.isLocal
inline val AstSimpleFunction.isLocal get() = status.visibility == Visibilities.Local

fun AstRegularClassBuilder.addDeclaration(declaration: AstDeclaration) {
    declarations += declaration
    if (companionObject == null && declaration is AstRegularClass && declaration.isCompanion) {
        companionObject = declaration
    }
}

fun AstRegularClassBuilder.addDeclarations(declarations: Collection<AstDeclaration>) {
    declarations.forEach(this::addDeclaration)
}

val AstClass<*>.classId get() = symbol.classId

fun AstFile.addDeclaration(declaration: AstDeclaration) {
    require(this is AstFileImpl)
    declarations += declaration
}

fun AstRegularClass.addDeclaration(declaration: AstDeclaration) {
    @Suppress("LiftReturnOrAssignment")
    when (this) {
        is AstRegularClassImpl -> declarations += declaration
        else -> throw IllegalStateException()
    }
}

val AstType.classOrFail: AstClassSymbol<*>
    get() = classOrNull ?: error("Could not get type for $this")

val AstType.classOrNull: AstClassSymbol<*>?
    get() = (this as? AstSimpleType)?.classifier as? AstClassSymbol<*>

fun AstAnnotatedDeclaration.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.annotationType.classOrFail.classId == classId }
}
