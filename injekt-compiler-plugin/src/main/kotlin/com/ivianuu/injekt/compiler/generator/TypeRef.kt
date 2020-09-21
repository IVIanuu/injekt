package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation

sealed class TypeRef {
    override fun equals(other: Any?) = compareTypeWithDistinct(this, other as? TypeRef)
    override fun hashCode() = hashWithDistinct()
}

class KotlinTypeRef(val kotlinType: KotlinType) : TypeRef()

class FqNameTypeRef(val fqName: FqName) : TypeRef()

private val TypeRef.classifierFqName: FqName
    get() = when (this) {
        is KotlinTypeRef -> kotlinType.getAbbreviation()?.constructor?.declarationDescriptor?.fqNameSafe
            ?: kotlinType.constructor.declarationDescriptor!!.fqNameSafe
        is FqNameTypeRef -> fqName
    }

private fun compareTypeWithDistinct(
    a: TypeRef?,
    b: TypeRef?
): Boolean = a?.hashWithDistinct() == b?.hashWithDistinct()

private fun TypeRef.hashWithDistinct(): Int {
    var result = 0
    val distinctedClassifier = classifierFqName
    result += 31 * distinctedClassifier.hashCode()
    if (this is KotlinTypeRef) {
        result += 31 * kotlinType.arguments.map { KotlinTypeRef(it.type).hashWithDistinct() }
            .hashCode()
    }

    val qualifier = if (this is KotlinTypeRef)
        kotlinType.annotations.findAnnotation(InjektFqNames.Qualifier)
            ?.allValueArguments?.values?.singleOrNull()?.value as? String
    else null
    if (qualifier != null) result += 31 * qualifier.hashCode()

    return result
}
