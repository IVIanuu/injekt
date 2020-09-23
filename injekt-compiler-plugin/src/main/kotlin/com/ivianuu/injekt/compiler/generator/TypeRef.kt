package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviation

sealed class TypeRef {
    abstract val fqName: FqName
    abstract val isMarkedNullable: Boolean
    abstract val isContext: Boolean
    abstract val isChildContextFactory: Boolean
    abstract val isGivenSet: Boolean
    abstract val typeArguments: List<TypeRef>
    abstract val variance: Variance
    abstract val isReader: Boolean
    abstract val isComposable: Boolean
    abstract val qualifier: String?
    private val typeName by unsafeLazy { uniqueTypeName() }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT
) : TypeRef() {
    override val fqName: FqName
        get() = kotlinType.prepare()
            .getAbbreviation()?.constructor?.declarationDescriptor?.fqNameSafe
            ?: kotlinType.prepare().constructor.declarationDescriptor!!.fqNameSafe
    override val isComposable: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.Composable)
    override val isReader: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.Reader)
    override val qualifier: String?
        get() = kotlinType.annotations.findAnnotation(InjektFqNames.Qualifier)
            ?.allValueArguments?.values?.singleOrNull()?.value as? String
    override val isContext: Boolean
        get() = kotlinType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ContextMarker)
    override val isChildContextFactory: Boolean
        get() = kotlinType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ChildContextFactory)
    override val isGivenSet: Boolean
        get() = kotlinType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.GivenSet)
    override val isMarkedNullable: Boolean
        get() = kotlinType.isMarkedNullable
    override val typeArguments: List<TypeRef>
        get() = kotlinType.arguments.map { KotlinTypeRef(it.type, it.projectionKind) }
}

class SimpleTypeRef(
    override val fqName: FqName,
    override val isMarkedNullable: Boolean = false,
    override val isContext: Boolean = false,
    override val isChildContextFactory: Boolean = false,
    override val isGivenSet: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isComposable: Boolean = false,
    override val isReader: Boolean = false,
    override val qualifier: String? = null
) : TypeRef()
