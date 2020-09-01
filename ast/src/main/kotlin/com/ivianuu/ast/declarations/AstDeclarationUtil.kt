/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations

import com.ivianuu.ast.declarations.builder.AstRegularClassBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParameterBuilder
import com.ivianuu.ast.declarations.impl.AstDefaultPropertyGetter
import com.ivianuu.ast.declarations.impl.AstDefaultPropertySetter
import com.ivianuu.ast.declarations.impl.AstFileImpl
import com.ivianuu.ast.declarations.impl.AstRegularClassImpl
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.ClassId

fun AstTypeParameterBuilder.addDefaultBoundIfNecessary(isFlexible: Boolean = false) {
    if (bounds.isEmpty()) {
        val type = if (isFlexible) {
            buildResolvedTypeRef {
                type = ConeFlexibleType(
                    session.builtinTypes.anyType.type,
                    session.builtinTypes.nullableAnyType.type
                )
            }
        } else {
            session.builtinTypes.nullableAnyType
        }
        bounds += type
    }
}

inline val AstRegularClass.isInner get() = status.isInner
inline val AstRegularClass.isCompanion get() = status.isCompanion
inline val AstRegularClass.isData get() = status.isData
inline val AstRegularClass.isInline get() = status.isInline
inline val AstRegularClass.isFun get() = status.isFun
inline val AstMemberDeclaration.modality get() = status.modality
inline val AstMemberDeclaration.visibility get() = status.visibility
inline val AstMemberDeclaration.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake
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
inline val AstPropertyAccessor.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake

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

val AstTypeAlias.expandedConeType: ConeClassLikeType? get() = expandedTypeRef.coneTypeSafe()

val AstClass<*>.classId get() = symbol.classId

val AstClassSymbol<*>.superConeTypes
    get() = when (this) {
        is AstRegularClassSymbol -> ast.superConeTypes
        is AstAnonymousObjectSymbol -> ast.superConeTypes
    }

val AstClass<*>.superConeTypes get() = superTypeRefs.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }

fun AstClass<*>.getPrimaryConstructorIfAny(): AstConstructor? =
    declarations.filterIsInstance<AstConstructor>().firstOrNull()?.takeIf { it.isPrimary }

fun AstRegularClass.collectEnumEntries(): Collection<AstEnumEntry> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<AstEnumEntry>()
}

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

private object IsFromVarargKey : AstDeclarationDataKey()

var AstProperty.isFromVararg: Boolean? by AstDeclarationDataRegistry.data(IsFromVarargKey)

private object IsReferredViaField : AstDeclarationDataKey()

var AstProperty.isReferredViaField: Boolean? by AstDeclarationDataRegistry.data(IsReferredViaField)

val AstProperty.hasBackingField: Boolean
    get() = initializer != null ||
            getter is AstDefaultPropertyGetter ||
            isVar && setter is AstDefaultPropertySetter ||
            delegate != null ||
            isReferredViaField == true

inline val AstProperty.hasJvmFieldAnnotation: Boolean
    get() = annotations.any {
        val classId = it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId
        classId?.packageFqName?.asString() == "kotlin.jvm" && classId.relativeClassName.asString() == "JvmField"
    }

fun AstAnnotatedDeclaration.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId == classId }
}

inline val AstDeclaration.isFromLibrary: Boolean
    get() = origin == AstDeclarationOrigin.Library
inline val AstDeclaration.isSynthetic: Boolean
    get() = origin == AstDeclarationOrigin.Synthetic
