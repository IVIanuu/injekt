package com.ivianuu.ast

import com.ivianuu.ast.symbols.StandardClassIds
import com.ivianuu.ast.symbols.impl.ConeClassLikeLookupTagImpl
import com.ivianuu.ast.types.ConeClassLikeType
import com.ivianuu.ast.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId

object PrimitiveTypes {
    val Boolean: ConeClassLikeType = StandardClassIds.Boolean.createType()
    val Char: ConeClassLikeType = StandardClassIds.Char.createType()
    val Byte: ConeClassLikeType = StandardClassIds.Byte.createType()
    val Short: ConeClassLikeType = StandardClassIds.Short.createType()
    val Int: ConeClassLikeType = StandardClassIds.Int.createType()
    val Long: ConeClassLikeType = StandardClassIds.Long.createType()
    val Float: ConeClassLikeType = StandardClassIds.Float.createType()
    val Double: ConeClassLikeType = StandardClassIds.Double.createType()
}

private fun ClassId.createType(): ConeClassLikeType =
    ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(this), emptyArray(), isNullable = false)

fun ConeClassLikeType.isDouble(): Boolean = lookupTag.classId == StandardClassIds.Double
fun ConeClassLikeType.isFloat(): Boolean = lookupTag.classId == StandardClassIds.Float
fun ConeClassLikeType.isLong(): Boolean = lookupTag.classId == StandardClassIds.Long
fun ConeClassLikeType.isInt(): Boolean = lookupTag.classId == StandardClassIds.Int
fun ConeClassLikeType.isShort(): Boolean = lookupTag.classId == StandardClassIds.Short
fun ConeClassLikeType.isByte(): Boolean = lookupTag.classId == StandardClassIds.Byte

fun ConeClassLikeType.isPrimitiveNumberType(): Boolean =
    lookupTag.classId in PRIMITIVE_NUMBER_CLASS_IDS

fun ConeClassLikeType.isPrimitiveUnsignedNumberType(): Boolean =
    lookupTag.classId in PRIMITIVE_UNSIGNED_NUMBER_CLASS_IDS

fun ConeClassLikeType.isPrimitiveNumberOrUnsignedNumberType(): Boolean =
    isPrimitiveNumberType() || isPrimitiveUnsignedNumberType()

private val PRIMITIVE_NUMBER_CLASS_IDS: Set<ClassId> = setOf(
    StandardClassIds.Double, StandardClassIds.Float, StandardClassIds.Long, StandardClassIds.Int,
    StandardClassIds.Short, StandardClassIds.Byte
)

private val PRIMITIVE_UNSIGNED_NUMBER_CLASS_IDS: Set<ClassId> = setOf(
    StandardClassIds.ULong, StandardClassIds.UInt, StandardClassIds.UShort, StandardClassIds.UByte
)
