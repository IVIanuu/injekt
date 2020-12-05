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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    // todo make read only
    var expandedType: TypeRef? = null,
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    var funApiParams: List<Name> = emptyList()
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
    isStarProjection: Boolean = false
) = KotlinTypeRef(this, variance, isStarProjection)

fun ClassifierDescriptor.toClassifierRef(): ClassifierRef = ClassifierRef(
    fqName = original.fqNameSafe,
    typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
        ?.map { it.toClassifierRef() } ?: emptyList(),
    superTypes = typeConstructor.supertypes.map { it.toTypeRef() },
    expandedType = (original as? TypeAliasDescriptor)?.expandedType
        ?.let { it.toTypeRef().fullyExpandedType },
    isTypeParameter = this is TypeParameterDescriptor,
    isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
    isTypeAlias = this is TypeAliasDescriptor,
    funApiParams = annotations.findAnnotation(InjektFqNames.FunApiParams)
        ?.allValueArguments
        ?.values
        ?.single()
        ?.let { it as ArrayValue }
        ?.value
        ?.map { it.value as String }
        ?.map { it.asNameId() }
        ?: emptyList()
)

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val typeArguments: List<TypeRef>
    abstract val variance: Variance
    abstract val isFunction: Boolean
    abstract val isSuspendFunction: Boolean
    abstract val isExtensionFunction: Boolean
    abstract val isModule: Boolean
    abstract val isBinding: Boolean
    abstract val isMergeComponent: Boolean
    abstract val isMergeChildComponent: Boolean
    abstract val isChildComponent: Boolean
    abstract val isComposable: Boolean
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<QualifierDescriptor>
    abstract val contributionKind: Callable.ContributionKind?
    abstract val targetComponent: TypeRef?
    abstract val scoped: Boolean
    abstract val eager: Boolean
    abstract val default: Boolean

    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }

    override fun toString(): String = typeName.asString()

    override fun equals(other: Any?) =
        other is TypeRef && other._hashCode == _hashCode

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        // todo result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + typeArguments.hashCode()
        result = 31 * result + variance.hashCode()
        result = 31 * result + isFunction.hashCode()
        result = 31 * result + isSuspendFunction.hashCode()
        result = 31 * result + isExtensionFunction.hashCode()
        result = 31 * result + isModule.hashCode()
        result = 31 * result + isBinding.hashCode()
        result = 31 * result + isMergeComponent.hashCode()
        result = 31 * result + isMergeChildComponent.hashCode()
        result = 31 * result + isChildComponent.hashCode()
        result = 31 * result + isComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
        result = 31 * result + qualifiers.hashCode()
        result
    }

    override fun hashCode(): Int =_hashCode

    val isComposableRecursive: Boolean by unsafeLazy {
        isComposable || expandedType?.isComposableRecursive == true ||
                superTypes().any { it.isComposableRecursive }
    }
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT,
    override val isStarProjection: Boolean = false,
) : TypeRef() {
    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.let { it.toClassifierRef() }
    }
    override val isFunction: Boolean by unsafeLazy {
        finalType.isFunctionType
    }
    override val isSuspendFunction: Boolean by unsafeLazy {
        finalType.isSuspendFunctionType
    }
    override val isExtensionFunction: Boolean by unsafeLazy {
        finalType.hasAnnotation(StandardNames.FqNames.extensionFunctionType)
    }
    override val isModule: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.Module)
    }
    override val isBinding: Boolean by unsafeLazy {
        (kotlinType.constructor.declarationDescriptor!! as? ClassDescriptor)
            ?.getInjectConstructor() != null
    }
    override val isMergeComponent: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.MergeComponent)
    }
    override val isMergeChildComponent: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.MergeChildComponent)
    }
    override val isChildComponent: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ChildComponent)
    }
    override val isComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val typeArguments: List<TypeRef> by unsafeLazy {
        finalType.arguments.map {
            it.type.toTypeRef(it.projectionKind, it.isStarProjection)
        }
    }
    override val qualifiers: List<QualifierDescriptor> by unsafeLazy {
        kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.toQualifierDescriptor() }
    }
    override val contributionKind: Callable.ContributionKind? by unsafeLazy {
        kotlinType.contributionKind()
    }
    override val targetComponent: TypeRef? by unsafeLazy {
        kotlinType.targetComponent(
            kotlinType.constructor.declarationDescriptor!!.module
        )
    }
    override val scoped: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Scoped)
    }
    override val eager: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Eager)
    }
    override val default: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Default)
    }
}

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isFunction: Boolean = false,
    override val isSuspendFunction: Boolean = false,
    override val isExtensionFunction: Boolean = false,
    override val isModule: Boolean = false,
    override val isBinding: Boolean = false,
    override val isMergeComponent: Boolean = false,
    override val isMergeChildComponent: Boolean = false,
    override val isChildComponent: Boolean = false,
    override val isComposable: Boolean = false,
    override val isStarProjection: Boolean = false,
    override val qualifiers: List<QualifierDescriptor> = emptyList(),
    override val contributionKind: Callable.ContributionKind? = null,
    override val targetComponent: TypeRef? = null,
    override val scoped: Boolean = false,
    override val eager: Boolean = false,
    override val default: Boolean = false
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
    isFunction: Boolean = this.isFunction,
    isSuspendFunction: Boolean = this.isSuspendFunction,
    isExtensionFunction: Boolean = this.isExtensionFunction,
    isModule: Boolean = this.isModule,
    isBinding: Boolean = this.isBinding,
    isMergeComponent: Boolean = this.isMergeComponent,
    isMergeChildComponent: Boolean = this.isMergeChildComponent,
    isChildComponent: Boolean = this.isChildComponent,
    isComposable: Boolean = this.isComposable,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<QualifierDescriptor> = this.qualifiers,
    contributionKind: Callable.ContributionKind? = this.contributionKind,
    targetComponent: TypeRef? = this.targetComponent,
    scoped: Boolean = this.scoped,
    eager: Boolean = this.eager,
    default: Boolean = this.default
) = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    typeArguments,
    variance,
    isFunction,
    isSuspendFunction,
    isExtensionFunction,
    isModule,
    isBinding,
    isMergeComponent,
    isMergeChildComponent,
    isChildComponent,
    isComposable,
    isStarProjection,
    qualifiers,
    contributionKind,
    targetComponent,
    scoped,
    eager,
    default
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    map[classifier]?.let {
        return it.copy(
            // we copy nullability to support T : Any? -> String
            isMarkedNullable = if (!isStarProjection) isMarkedNullable else it.isMarkedNullable,
            // we copy qualifiers to support @MyQualifier T -> @MyQualifier String
            qualifiers = qualifiers + it.qualifiers
        )
    }

    if (typeArguments.isEmpty() && qualifiers.isEmpty() &&
        !classifier.isTypeParameter) return this

    val substituted = copy(
        typeArguments = typeArguments.map { it.substitute(map) },
        qualifiers = qualifiers.map { it.substitute(map) }
    )

    if (classifier.isTypeParameter && substituted == this) {
        val superType = classifier.superTypes.singleOrNull() // todo support multiple
        if (superType != null) {
            val substitutedSuperType = superType.substitute(map)
            if (substitutedSuperType != superType) return substitutedSuperType
        }
    }

    return substituted
}

val STAR_PROJECTION_TYPE = SimpleTypeRef(
    classifier = ClassifierRef(StandardNames.FqNames.any.toSafe()),
    isStarProjection = true
)

fun TypeRef.replaceTypeParametersWithStars(): TypeRef {
    if (classifier.isTypeParameter) return STAR_PROJECTION_TYPE
    if (typeArguments.isEmpty() && expandedType == null) return this
    return copy(typeArguments = typeArguments.map { it.replaceTypeParametersWithStars() })
}

fun TypeRef.substituteStars(baseType: TypeRef): TypeRef {
    if (this == baseType) return this
    if (isStarProjection && !baseType.classifier.isTypeParameter) return baseType
    if (classifier != baseType.classifier) return this
    if (typeArguments.isEmpty()) return this
    return copy(
        typeArguments = typeArguments
            .zip(baseType.typeArguments)
            .map { (thisTypeArgument, baseTypeArgument) ->
                thisTypeArgument.substituteStars(baseTypeArgument)
            }
    )
}

fun TypeRef.render(expanded: Boolean = false): String {
    return buildString {
        fun TypeRef.inner() {
            val annotations = (if (!expanded) qualifiers.map {
                "@${it.type}(${it.args.toList().joinToString { "${it.first}=${it.second}" }})"
            } else emptyList()) + listOfNotNull(
                if (isComposable) "@${InjektFqNames.Composable}" else null,
                if (isExtensionFunction) "@ExtensionFunctionType" else null
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
                        !typeArgument.isStarProjection)
                        append("${typeArgument.variance.label} ")
                    append(typeArgument.render(expanded = expanded))
                    if (index != typeArguments.lastIndex) append(", ")
                }
                append(">")
            }
            if (isMarkedNullable && !isStarProjection) append("?")
        }
        if (expanded) fullyExpandedType.inner() else inner()
    }
}

fun TypeRef.uniqueTypeName(includeNullability: Boolean = true): Name {
    fun TypeRef.renderName(includeArguments: Boolean = true): String {
        return buildString {
            qualifiers.forEach {
                append(it.type.uniqueTypeName())
                append(it.args.hashCode())
                append("_")
            }
            if (isComposable) append("composable_")
            // if (includeNullability && isMarkedNullable) append("nullable_")
            if (isStarProjection) append("star")
            else append(classifier.fqName.pathSegments().joinToString("_") { it.asString() })
            if (includeArguments) {
                typeArguments.forEachIndexed { index, typeArgument ->
                    if (index == 0) append("_")
                    append(typeArgument.renderName())
                    if (index != typeArguments.lastIndex) append("_")
                }
            }
        }
    }

    val fullTypeName = renderName()

    // Conservatively shorten the name if the length exceeds 128
    return (
            if (fullTypeName.length <= 128) fullTypeName
            else ("${renderName(includeArguments = false)}_${fullTypeName.hashCode()}")
            )
        .removeIllegalChars()
        .asNameId()
}

fun getSubstitutionMap(
    pairs: List<Pair<TypeRef, TypeRef>>,
    typeParameters: List<ClassifierRef> = emptyList()
): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()

    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (baseType.classifier.isTypeParameter) {
            substitutionMap[baseType.classifier] = thisType
            baseType.superTypes()
                .map { it.fullyExpandedType }
                .map { thisType.subtypeView(it.classifier, substitutionMap) to it }
                .forEach { (thisBaseTypeView, baseSuperType) ->
                    thisBaseTypeView?.typeArguments?.zip(baseSuperType.typeArguments)
                        ?.forEach { visitType(it.first, it.second) }
                }
        } else {
            thisType.typeArguments.zip(baseType.typeArguments)
                .forEach { visitType(it.first, it.second) }
            thisType.qualifiers.zip(baseType.qualifiers)
                .forEach { visitType(it.first.type, it.second.type) }
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

fun TypeRef.getStarSubstitutionMap(baseType: TypeRef): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()

    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (baseType.isStarProjection && !thisType.isStarProjection && !thisType.classifier.isTypeParameter) {
            substitutionMap[baseType.classifier] = thisType
        } else {
            thisType.typeArguments.zip(baseType.typeArguments).forEach {
                visitType(it.first, it.second)
            }
        }
    }

    visitType(this, baseType)

    return substitutionMap
}

fun TypeRef.isAssignable(
    superType: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap()
): Boolean {
    if (isStarProjection || superType.isStarProjection) return true

    if (this == superType &&
        !isMarkedNullable && superType.isMarkedNullable) return false

    if (!qualifiers.isAssignable(superType.qualifiers)) return false
    if (classifier.fqName == superType.classifier.fqName &&
        isComposableRecursive != superType.isComposableRecursive) return false

    if (superType.classifier.isTypeParameter) {
        return superType.superTypes(substitutionMap).all { upperBound ->
            isSubTypeOf(upperBound, substitutionMap)
        }
    }

    if (classifier.fqName != superType.classifier.fqName) return false

    if (!typeArguments.zip(superType.typeArguments).all { (a, b) -> a.isAssignable(b, substitutionMap) })
        return false

    return true
}

fun TypeRef.isSubTypeOf(
    superType: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap()
): Boolean {
    if (isMarkedNullable && !superType.isMarkedNullable) return false
    if (this == superType && (!isMarkedNullable || superType.isMarkedNullable) &&
        (superType.qualifiers.isEmpty() || qualifiers.isAssignable(superType.qualifiers))
    ) return true
    if (superType.classifier.fqName.asString() == StandardNames.FqNames.any.asString() &&
        (!isMarkedNullable || superType.isMarkedNullable) &&
        (superType.qualifiers.isEmpty() || qualifiers.isAssignable(superType.qualifiers)))
        return true

    if (classifier == superType.classifier && (!isMarkedNullable || superType.isMarkedNullable) &&
        isComposableRecursive != superType.isComposableRecursive) return false
    val subTypeView = subtypeView(superType.classifier, substitutionMap)
    if (subTypeView != null) {
        if (subTypeView == superType && (!subTypeView.isMarkedNullable || superType.isMarkedNullable) &&
            (superType.qualifiers.isEmpty() || subTypeView.qualifiers.isAssignable(superType.qualifiers)))
            return true
        return subTypeView.typeArguments.zip(superType.typeArguments).all { (subTypeArg, superTypeArg) ->
            superTypeArg.superTypes(substitutionMap).all {
                subTypeArg.isSubTypeOf(it, substitutionMap)
            }
        }
    } else if (superType.classifier.isTypeParameter ||
        superType.classifier.isTypeAlias) {
        return superType.superTypes(substitutionMap).all { upperBound ->
            isSubTypeOf(upperBound, substitutionMap)
        }
    }
    return false
}

fun List<QualifierDescriptor>.isAssignable(superQualifiers: List<QualifierDescriptor>): Boolean {
    if (size != superQualifiers.size) return false
    return zip(superQualifiers).all { (thisQualifier, superQualifier) ->
        thisQualifier.isAssignable(superQualifier)
    }
}

fun QualifierDescriptor.isAssignable(superQualifier: QualifierDescriptor): Boolean {
    if (!type.isAssignable(superQualifier.type)) return false
    return args == superQualifier.args
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
