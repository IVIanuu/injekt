/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.types

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.types.impl.AstImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId
import kotlin.contracts.ExperimentalContracts

inline fun <reified T : ConeKotlinType> AstTypeRef.coneTypeUnsafe() =
    (this as AstResolvedTypeRef).type as T

@OptIn(ExperimentalContracts::class)
inline fun <reified T : ConeKotlinType> AstTypeRef.coneTypeSafe(): T? {
    contract {
        returnsNotNull() implies (this@coneTypeSafe is AstResolvedTypeRef)
    }
    return (this as? AstResolvedTypeRef)?.type as? T
}

inline val AstTypeRef.coneType: ConeKotlinType get() = coneTypeUnsafe()

val AstTypeRef.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val AstTypeRef.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val AstTypeRef.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val AstTypeRef.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val AstTypeRef.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)
val AstTypeRef.isBoolean: Boolean get() = isBuiltinType(StandardClassIds.Boolean, false)
val AstTypeRef.isEnum: Boolean get() = isBuiltinType(StandardClassIds.Enum, false)
val AstTypeRef.isArrayType: Boolean
    get() =
        isBuiltinType(StandardClassIds.Array, false) ||
                StandardClassIds.primitiveArrayTypeByElementType.values.any {
                    isBuiltinType(
                        it,
                        false
                    )
                }

private val AstTypeRef.classLikeTypeOrNull: ConeClassLikeType?
    get() = when (this) {
        is AstImplicitBuiltinTypeRef -> type
        is AstResolvedTypeRef -> type as? ConeClassLikeType
        else -> null
    }

private fun AstTypeRef.isBuiltinType(classId: ClassId, isNullable: Boolean): Boolean {
    val type = this.classLikeTypeOrNull ?: return false
    return type.lookupTag.classId == classId && type.isNullable == isNullable
}

val AstTypeRef.isMarkedNullable: Boolean?
    get() = classLikeTypeOrNull?.isMarkedNullable

val AstFunctionTypeRef.parametersCount: Int
    get() = if (receiverTypeRef != null)
        valueParameters.size + 1
    else
        valueParameters.size

const val EXTENSION_FUNCTION_ANNOTATION = "kotlin/ExtensionFunctionType"

val AstAnnotationCall.isExtensionFunctionAnnotationCall: Boolean
    get() = (this as? AstAnnotationCall)?.let {
        (it.annotationTypeRef as? AstResolvedTypeRef)?.let {
            (it.type as? ConeClassLikeType)?.let {
                it.lookupTag.classId.asString() == EXTENSION_FUNCTION_ANNOTATION
            }
        }
    } == true


fun List<AstAnnotationCall>.dropExtensionFunctionAnnotation(): List<AstAnnotationCall> {
    return filterNot { it.isExtensionFunctionAnnotationCall }
}

fun ConeClassLikeType.toConstKind(): AstConstKind<*>? = when (lookupTag.classId) {
    StandardClassIds.Byte -> AstConstKind.Byte
    StandardClassIds.Short -> AstConstKind.Short
    StandardClassIds.Int -> AstConstKind.Int
    StandardClassIds.Long -> AstConstKind.Long

    StandardClassIds.UInt -> AstConstKind.UnsignedInt
    StandardClassIds.ULong -> AstConstKind.UnsignedLong
    StandardClassIds.UShort -> AstConstKind.UnsignedShort
    StandardClassIds.UByte -> AstConstKind.UnsignedByte
    else -> null
}

fun List<AstAnnotationCall>.computeTypeAttributes(): ConeAttributes {
    if (this.isEmpty()) return ConeAttributes.Empty
    val attributes = mutableListOf<ConeAttribute<*>>()
    for (annotation in this) {
        val type = annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
        when (type.lookupTag.classId) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.ExtensionFunctionType
        }
    }
    return ConeAttributes.create(attributes)
}