package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType

sealed class TypeRef {
    private val typeName by unsafeLazy { uniqueTypeName() }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinTypeRef(val kotlinType: KotlinType) : TypeRef()

class FqNameTypeRef(
    val fqName: FqName,
    val typeArguments: List<TypeRef>
) : TypeRef()
