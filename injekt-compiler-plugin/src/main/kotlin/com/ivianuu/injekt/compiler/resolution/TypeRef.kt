/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.prepare
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val expandedType: TypeRef? = null,
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val isGivenFunAlias: Boolean = false,
    val descriptor: ClassifierDescriptor? = null
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()
}

val TypeRef.expandedType: TypeRef?
    get() {
        return classifier.expandedType?.let {
            val substitutionMap = classifier.typeParameters
                .zip(typeArguments)
                .toMap()
            it.substitute(substitutionMap)
                .copy(isMarkedNullable = isMarkedNullable)
        }
    }

val ClassifierRef.defaultType: TypeRef
    get() = SimpleTypeRef(
        this,
        typeArguments = typeParameters.map { it.defaultType }
    )

fun TypeRef.superTypes(substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap()): List<TypeRef> {
    val merged = classifier.typeParameters
        .zip(typeArguments)
        .toMap() + substitutionMap
    return classifier.superTypes
        .map { it.substitute(merged) }
}

fun KotlinType.toTypeRef(
    variance: Variance = Variance.INVARIANT,
    isStarProjection: Boolean = false,
) = KotlinTypeRef(this, variance, isStarProjection)

fun ClassifierDescriptor.toClassifierRef(): ClassifierRef = ClassifierRef(
    fqName = original.fqNameSafe,
    typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
        ?.map { it.toClassifierRef() } ?: emptyList(),
    superTypes = typeConstructor.supertypes.map {
        it.toTypeRef().applyTypeParameterFixIfNeeded(this)
    },
    expandedType = (original as? TypeAliasDescriptor)?.expandedType?.toTypeRef()?.fullyExpandedType,
    isTypeParameter = this is TypeParameterDescriptor,
    isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
    isTypeAlias = this is TypeAliasDescriptor,
    isGivenFunAlias = this is TypeAliasDescriptor && hasAnnotation(InjektFqNames.GivenFunAlias),
    descriptor = this
)

private fun TypeRef.applyTypeParameterFixIfNeeded(
    descriptor: ClassifierDescriptor
): TypeRef {
    if (descriptor !is TypeParameterDescriptor) return this
    if (!descriptor.isExternalDeclaration()) return this
    val parent = descriptor.containingDeclaration as? FunctionDescriptor ?: return this
    val fix = parent.annotations.findAnnotation(InjektFqNames.TypeParameterFix)
        ?: return this
    val name = fix.allValueArguments["typeParameter".asNameId()]!!.value as String
    if (name != descriptor.name.asString()) return this
    val qualifierType = fix.allValueArguments["qualifier".asNameId()]
        .let { it as KClassValue }
        .getArgumentType(descriptor.module)
        .replace(
            newArguments = fix.allValueArguments["qualifierTypeParameters".asNameId()]!!
                .let { it as ArrayValue }
                .value
                .map { it.value as String }
                .map { typeParameterName ->
                    parent.typeParameters
                        .single { it.name.asString() == typeParameterName }
                        .defaultType
                        .asTypeProjection()
                }
        )
    return copy(
        qualifiers = listOf(
            AnnotationDescriptorImpl(
                qualifierType,
                emptyMap(),
                SourceElement.NO_SOURCE
            )
        )
    )
}

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val typeArguments: List<TypeRef>
    abstract val variance: Variance
    abstract val isComposable: Boolean
    abstract val contributionKind: ContributionKind?
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<AnnotationDescriptor>
    abstract val unqualified: Boolean
    abstract val path: List<Any>?

    private val typeName by unsafeLazy { uniqueTypeName() }

    override fun toString(): String = typeName

    override fun equals(other: Any?) =
        other is TypeRef && other.typeName == typeName

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        // todo result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + typeArguments.hashCode()
        // todo result result = 31 * result + variance.hashCode()
        result = 31 * result + isComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
        result = 31 * result + qualifiers
            .map { it.type.toTypeRef() to it.allValueArguments }
            .hashCode()
        result = 31 * result + path.hashCode()

        result
    }

    override fun hashCode(): Int = _hashCode

    val thisAndAllSuperTypes: Set<TypeRef> by unsafeLazy {
        buildSet<TypeRef> {
            this += this@TypeRef
            expandedType?.let { this += it.thisAndAllSuperTypes }
            this += superTypes().flatMap { it.thisAndAllSuperTypes }
        }
    }

    val typeSize: Int by unsafeLazy {
        var typeSize = 0
        val seen = mutableSetOf<TypeRef>()
        fun visit(type: TypeRef) {
            typeSize++
            if (type in seen) return
            seen += type
            type.qualifiers.forEach { visit(it.type.toTypeRef()) }
            type.typeArguments.forEach { visit(it) }
        }
        visit(this)
        typeSize
    }

    val coveringSet: Set<ClassifierRef> by unsafeLazy {
        val classifiers = mutableSetOf<ClassifierRef>()
        val seen = mutableSetOf<TypeRef>()
        fun visit(type: TypeRef) {
            if (type in seen) return
            seen += type
            classifiers += type.classifier
            type.qualifiers.forEach { visit(it.type.toTypeRef()) }
            type.typeArguments.forEach { visit(it) }
        }
        visit(this)
        classifiers
    }

    val allTypes: Set<TypeRef> by unsafeLazy {
        buildSet<TypeRef> {
            this += this@TypeRef
            expandedType?.let { this += it.allTypes }
            this += superTypes().flatMap { it.allTypes }
        }
    }
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT,
    override val isStarProjection: Boolean = false,
) : TypeRef() {
    init {
        check(!kotlinType.isError) {
            "Error type $kotlinType ${kotlinType.javaClass}"
        }
    }

    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.toClassifierRef()
    }
    override val isComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val contributionKind: ContributionKind?
        get() = finalType.contributionKind() ?: kotlinType.contributionKind()
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val typeArguments: List<TypeRef> by unsafeLazy {
        finalType.arguments.map {
            it.type.toTypeRef(it.projectionKind, it.isStarProjection)
        }
    }
    override val unqualified: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.Unqualified)
    override val qualifiers: List<AnnotationDescriptor> by unsafeLazy {
        if (!unqualified)
            kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
        else emptyList()
    }
    override val path: List<Any>? get() = null
}

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isComposable: Boolean = false,
    override val contributionKind: ContributionKind? = null,
    override val isStarProjection: Boolean = false,
    override val qualifiers: List<AnnotationDescriptor> = emptyList(),
    override val unqualified: Boolean = false,
    override val path: List<Any>? = null,
) : TypeRef() {
    init {
        check(typeArguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${typeArguments.map { it.render() }}"
        }
    }
}

fun TypeRef.typeWith(typeArguments: List<TypeRef>): TypeRef = copy(typeArguments = typeArguments)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    typeArguments: List<TypeRef> = this.typeArguments,
    variance: Variance = this.variance,
    isComposable: Boolean = this.isComposable,
    contributionKind: ContributionKind? = this.contributionKind,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<AnnotationDescriptor> = this.qualifiers,
    unqualified: Boolean = this.unqualified,
    path: List<Any>? = this.path,
) = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    typeArguments,
    variance,
    isComposable,
    contributionKind,
    isStarProjection,
    qualifiers,
    unqualified,
    path
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    if (map.isEmpty()) return this
    map[classifier]?.let {
        return it.copy(
            // we copy nullability to support T : Any? -> String
            isMarkedNullable = if (!isStarProjection) isMarkedNullable else it.isMarkedNullable,
            // we copy qualifiers to support @MyQualifier T -> @MyQualifier String
            qualifiers = if (unqualified) emptyList() else qualifiers + it.qualifiers,
            // we copy given kind to support @Given C -> @Given String
            // fallback to substitution given kind
            contributionKind = contributionKind ?: it.contributionKind
        )
    }

    if (typeArguments.isEmpty() && qualifiers.isEmpty() &&
        !classifier.isTypeParameter
    ) return this

    val substituted = copy(
        typeArguments = typeArguments.map { it.substitute(map) }
    )

    if (classifier.isTypeParameter && substituted == this) {
        classifier
            .superTypes
            .firstNotNullResult {
                val substitutedSuperType = it.substitute(map)
                if (substitutedSuperType != it) substitutedSuperType
                else null
            }
            ?.let { return it }
    }

    return substituted
}

val STAR_PROJECTION_TYPE = SimpleTypeRef(
    classifier = ClassifierRef(StandardNames.FqNames.any.toSafe()),
    isStarProjection = true
)

fun TypeRef.render(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        fun TypeRef.inner() {
            val annotations = qualifiers.map {
                "@${it.type}(${
                    it.allValueArguments.toList().joinToString { "${it.first}=${it.second}" }
                })"
            } + listOfNotNull(
                if (isComposable) "@${InjektFqNames.Composable}" else null,
            )
            if (annotations.isNotEmpty()) {
                annotations.forEach { annotation ->
                    append(annotation)
                    append(" ")
                }
            }
            when {
                classifier.isTypeParameter -> append(classifier.fqName.shortName())
                isStarProjection -> append("*")
                else -> append(classifier.fqName)
            }
            if (typeArguments.isNotEmpty()) {
                append("<")
                typeArguments.forEachIndexed { index, typeArgument ->
                    if (typeArgument.variance != Variance.INVARIANT &&
                        !typeArgument.isStarProjection
                    )
                        append("${typeArgument.variance.label} ")
                    append(typeArgument.render(depth = depth + 1))
                    if (index != typeArguments.lastIndex) append(", ")
                }
                append(">")
            }
            if (isMarkedNullable && !isStarProjection) append("?")
        }
        inner()
    }
}

fun TypeRef.uniqueTypeName(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        qualifiers.forEach {
            append(it.type.constructor.declarationDescriptor!!.fqNameSafe)
            append(it.allValueArguments.hashCode())
            append("_")
        }
        if (isComposable) append("composable_")
        // if (includeNullability && isMarkedNullable) append("nullable_")
        if (isStarProjection) append("star")
        else append(classifier.fqName.pathSegments().joinToString("_") { it.asString() })
        typeArguments.forEachIndexed { index, typeArgument ->
            if (index == 0) append("_")
            append(typeArgument.uniqueTypeName(depth + 1))
            if (index != typeArguments.lastIndex) append("_")
        }
    }
}

fun getSubstitutionMap(
    pairs: List<Pair<TypeRef, TypeRef>>,
    typeParameters: List<ClassifierRef> = emptyList(),
): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()
    val visitedTypes = mutableSetOf<TypeRef>()
    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (thisType in visitedTypes && baseType in visitedTypes) {
            return
        }
        visitedTypes += thisType
        visitedTypes += baseType
        if (baseType.classifier.isTypeParameter) {
            if (baseType.classifier !in substitutionMap) {
                substitutionMap[baseType.classifier] = thisType
            }
            baseType.superTypes()
                .map { thisType.subtypeView(it.classifier, substitutionMap) to it }
                .forEach { (thisBaseTypeView, baseSuperType) ->
                    if (baseSuperType.classifier.isTypeParameter) {
                        val thisTypeToUse = thisBaseTypeView ?: thisType
                        visitType(thisTypeToUse
                            .copy(qualifiers = emptyList()), baseSuperType)
                        if (thisTypeToUse.qualifiers.isAssignableTo(baseSuperType.qualifiers)) {
                            thisTypeToUse.qualifiers.zip(baseSuperType.qualifiers)
                                .map { it.first.type.toTypeRef() to it.second.type.toTypeRef() }
                                .forEach { visitType(it.first, it.second) }
                        }
                    } else {
                        visitType(thisBaseTypeView ?: thisType, baseSuperType)
                    }

                    thisBaseTypeView?.typeArguments?.zip(baseSuperType.typeArguments)
                        ?.forEach { visitType(it.first, it.second) }

                    if (thisType.qualifiers.isAssignableTo(baseSuperType.qualifiers)) {
                        thisType.qualifiers.zip(baseSuperType.qualifiers)
                            .map { it.first.type.toTypeRef() to it.second.type.toTypeRef() }
                            .forEach { visitType(it.first, it.second) }
                    }
                }
        } else {
            thisType.typeArguments.zip(baseType.typeArguments)
                .forEach { visitType(it.first, it.second) }
        }
    }
    var lastSubstitutionMap: Map<ClassifierRef, TypeRef>? = null
    while (lastSubstitutionMap != substitutionMap) {
        pairs.forEach { visitType(it.first, it.second) }
        substitutionMap.forEach { visitType(it.value, it.key.defaultType) }
        lastSubstitutionMap = substitutionMap.toMap()
    }
    typeParameters
        .filter { it !in substitutionMap }
        .forEach { typeParameter ->
            substitutionMap[typeParameter] = typeParameter.defaultType.substitute(substitutionMap)
        }
    return substitutionMap
}

fun TypeRef.isAssignableTo(
    superType: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap(),
): Boolean {
    if (isStarProjection || superType.isStarProjection) return true
    if (classifier.fqName == superType.classifier.fqName) {
        if (isMarkedNullable && !superType.isMarkedNullable) return false
        if (!qualifiers.isAssignableTo(superType.qualifiers)) return false
        if (path != superType.path) return false
        if (thisAndAllSuperTypes.any { it.isComposable } != superType.thisAndAllSuperTypes.any { it.isComposable }) return false
        if (typeArguments.zip(superType.typeArguments)
                .any { (a, b) -> !a.isAssignableTo(b, substitutionMap) }
        )
            return false
        return true
    }
    if (superType.classifier.isTypeParameter) {
        val superTypesAssignable = superType.superTypes(substitutionMap).all { upperBound ->
            isSubTypeOf(upperBound, substitutionMap)
        }
        if (!superTypesAssignable) return false
        if (superType.unqualified && qualifiers.isNotEmpty()) return false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(superType.qualifiers)
        ) return false
        return true
    } else if (classifier.isTypeParameter) {
        val superTypesAssignable = superTypes(substitutionMap).all { upperBound ->
            superType.isSubTypeOf(upperBound, substitutionMap)
        }
        if (!superTypesAssignable) return false
        if (unqualified && superType.qualifiers.isNotEmpty()) return false
        if (qualifiers.isNotEmpty() &&
            !superType.qualifiers.isAssignableTo(qualifiers)
        ) return false
        return true
    }
    return false
}

fun TypeRef.isSubTypeOf(
    superType: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap(),
): Boolean {
    if (isStarProjection) return true
    if (classifier.fqName == superType.classifier.fqName) {
        if (isMarkedNullable && !superType.isMarkedNullable) return false
        if (unqualified && superType.qualifiers.isNotEmpty()) return false
        if (!qualifiers.isAssignableTo(superType.qualifiers)) return false
        if (path != superType.path) return false
        if (thisAndAllSuperTypes.any { it.isComposable } != superType.thisAndAllSuperTypes.any { it.isComposable }) return false
        if (typeArguments.zip(superType.typeArguments)
                .any { (a, b) -> !a.isAssignableTo(b, substitutionMap) }
        )
            return false
        return true
    }
    if (superType.classifier.fqName == InjektFqNames.Any) {
        if (isMarkedNullable && !superType.isMarkedNullable) return false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(superType.qualifiers)
        ) return false
        if (superType.unqualified && qualifiers.isNotEmpty()) return false
        return true
    }
    val subTypeView = subtypeView(superType.classifier, substitutionMap)
    if (subTypeView != null) {
        if (subTypeView == superType && (!subTypeView.isMarkedNullable || superType.isMarkedNullable) &&
            (superType.qualifiers.isEmpty() || subTypeView.qualifiers.isAssignableTo(superType.qualifiers))
        )
            return true
        return subTypeView.typeArguments.zip(superType.typeArguments)
            .all { (subTypeArg, superTypeArg) ->
                superTypeArg.superTypes(substitutionMap).all {
                    subTypeArg.isSubTypeOf(it, substitutionMap)
                }
            }
    } else if (superType.classifier.isTypeParameter ||
        superType.classifier.isTypeAlias
    ) {
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(superType.qualifiers)
        ) return false
        if (superType.unqualified && qualifiers.isNotEmpty()) return false
        return superType.superTypes(substitutionMap).all { upperBound ->
            isSubTypeOf(upperBound, substitutionMap)
        }
    }
    return false
}

fun List<AnnotationDescriptor>.isAssignableTo(superQualifiers: List<AnnotationDescriptor>): Boolean {
    if (size != superQualifiers.size) return false
    return zip(superQualifiers).all { (thisQualifier, superQualifier) ->
        thisQualifier.isAssignableTo(superQualifier)
    }
}

fun AnnotationDescriptor.isAssignableTo(superQualifier: AnnotationDescriptor): Boolean {
    if (!type.toTypeRef().isAssignableTo(superQualifier.type.toTypeRef())) return false
    return allValueArguments == superQualifier.allValueArguments
}

val TypeRef.fullyExpandedType: TypeRef
    get() = expandedType?.fullyExpandedType ?: this

fun TypeRef.subtypeView(
    classifier: ClassifierRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap(),
): TypeRef? {
    if (this.classifier == classifier) return this
    expandedType?.subtypeView(classifier, substitutionMap)?.let { return it }
    for (superType in superTypes(substitutionMap)) {
        superType.subtypeView(classifier, substitutionMap)?.let { return it }
    }
    return null
}
