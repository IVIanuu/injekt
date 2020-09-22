package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

sealed class TypeRef {
    abstract val isMarkedNullable: Boolean
    private val typeName by unsafeLazy { uniqueTypeName() }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinTypeRef(val kotlinType: KotlinType) : TypeRef() {
    override val isMarkedNullable: Boolean
        get() = kotlinType.isMarkedNullable
}

class FqNameTypeRef(
    val fqName: FqName,
    override val isMarkedNullable: Boolean = false,
    val typeArguments: List<TypeRef> = emptyList()
) : TypeRef()

val TypeRef.fqName: FqName
    get() = when (this) {
        is KotlinTypeRef -> kotlinType.prepare().constructor.declarationDescriptor!!.fqNameSafe
        is FqNameTypeRef -> fqName
    }