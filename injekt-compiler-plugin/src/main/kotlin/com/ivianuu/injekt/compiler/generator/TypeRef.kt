package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

sealed class TypeRef {
    abstract val fqName: FqName
    abstract val isMarkedNullable: Boolean
    abstract val isContext: Boolean
    abstract val isChildContextFactory: Boolean
    abstract val typeArguments: List<TypeRef>
    private val typeName by unsafeLazy { uniqueTypeName() }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinTypeRef(val kotlinType: KotlinType) : TypeRef() {
    override val fqName: FqName
        get() = kotlinType.prepare().constructor.declarationDescriptor!!.fqNameSafe
    override val isContext: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.ContextMarker)
    override val isChildContextFactory: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.ChildContextFactory)
    override val isMarkedNullable: Boolean
        get() = kotlinType.isMarkedNullable
    override val typeArguments: List<TypeRef>
        get() = kotlinType.arguments.map { KotlinTypeRef(it.type) }
}

class SimpleTypeRef(
    override val fqName: FqName,
    override val isMarkedNullable: Boolean = false,
    override val isContext: Boolean = false,
    override val isChildContextFactory: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList()
) : TypeRef()
