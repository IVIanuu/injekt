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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    // todo make this immutable
    var superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val argName: Name? = null
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()
}

val ClassifierRef.defaultType: TypeRef
    get() = SimpleTypeRef(
        this,
        typeArguments = typeParameters.map { it.defaultType },
        superTypes = superTypes,
        expandedType = if (isTypeAlias) superTypes.single() else null
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
    abstract val superTypes: List<TypeRef>
    abstract val expandedType: TypeRef?
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<QualifierDescriptor>
    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
    override fun toString(): String = render()
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    val typeTranslator: TypeTranslator,
    override val variance: Variance = Variance.INVARIANT,
    override val isStarProjection: Boolean = false,
) : TypeRef() {
    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.let {
            typeTranslator.toClassifierRef(it, fixType = false)
        }
    }
    override val isFunction: Boolean by unsafeLazy {
        finalType.isFunctionType
    }
    override val isSuspendFunction: Boolean by unsafeLazy {
        finalType.isSuspendFunctionType
    }
    override val isExtensionFunction: Boolean by unsafeLazy {
        finalType.hasAnnotation(KotlinBuiltIns.FQ_NAMES.extensionFunctionType)
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
    override val superTypes: List<TypeRef> by unsafeLazy {
        kotlinType.constructor.supertypes.map {
            typeTranslator.toTypeRef(it,
                finalType.constructor.declarationDescriptor,
                fixType = false)
        }
    }
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val typeArguments: List<TypeRef> by unsafeLazy {
        finalType.arguments.map {
            typeTranslator.toTypeRef(it.type,
                finalType.constructor.declarationDescriptor,
                it.projectionKind,
                it.isStarProjection,
                false)
        }
    }
    override val expandedType: TypeRef? by unsafeLazy {
        (kotlinType.constructor.declarationDescriptor as? TypeAliasDescriptor)
            ?.expandedType?.let {
                typeTranslator.toTypeRef(it,
                    finalType.constructor.declarationDescriptor,
                    fixType = false)
            }
            ?: kotlinType.getAbbreviatedType()?.expandedType?.let {
                typeTranslator.toTypeRef(it,
                    finalType.constructor.declarationDescriptor,
                    fixType = false)
            }
    }
    override val qualifiers: List<QualifierDescriptor> by lazy {
        kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map {
                typeTranslator.declarationStore.qualifierDescriptorForAnnotation(
                    it,
                    kotlinType.constructor.declarationDescriptor
                )
            }
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
    override val superTypes: List<TypeRef> = emptyList(),
    override val expandedType: TypeRef? = null,
    override val isStarProjection: Boolean = false,
    override val qualifiers: List<QualifierDescriptor> = emptyList()
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
    superTypes: List<TypeRef> = this.superTypes,
    expandedType: TypeRef? = this.expandedType,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<QualifierDescriptor> = this.qualifiers
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
    superTypes,
    expandedType,
    isStarProjection,
    qualifiers
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    map[classifier]?.let {
        // we copy qualifiers to support @MyQualifier T -> @MyQualifier String
        return it.copy(qualifiers = qualifiers)
    }

    val substituted = copy(
        typeArguments = typeArguments.map { it.substitute(map) },
        expandedType = expandedType?.substitute(map)
    )

    if (classifier.isTypeParameter && substituted == this) {
        val superType = classifier.defaultType.superTypes.singleOrNull() // todo support multiple
        if (superType != null) {
            val substitutedSuperType = superType.substitute(map)
            if (substitutedSuperType != superType) return substitutedSuperType
        }
    }

    return substituted
}

val STAR_PROJECTION_TYPE = SimpleTypeRef(
    classifier = ClassifierRef(KotlinBuiltIns.FQ_NAMES.any.toSafe()),
    isStarProjection = true
)

fun TypeRef.replaceTypeParametersWithStars(): TypeRef {
    if (classifier.isTypeParameter) return STAR_PROJECTION_TYPE
    if (typeArguments.isEmpty() && expandedType == null) return this
    return copy(
        typeArguments = typeArguments.map { it.replaceTypeParametersWithStars() },
        expandedType = expandedType?.replaceTypeParametersWithStars()
    )
}

fun TypeRef.substituteStars(baseType: TypeRef): TypeRef {
    if (this == baseType) return this
    if (isStarProjection && !baseType.classifier.isTypeParameter) return baseType
    if (classifier != baseType.classifier) return this
    return copy(
        typeArguments = typeArguments
            .zip(baseType.typeArguments)
            .map { (thisTypeArgument, baseTypeArgument) ->
                thisTypeArgument.substituteStars(baseTypeArgument)
            }
    )
}

fun TypeRef.render(): String {
    return buildString {
        val annotations = qualifiers.map {
            "@${it.type}(${it.args.toList().joinToString { "${it.first}=${it.second}" }})"
        } + listOfNotNull(
            if (isComposable) "@${InjektFqNames.Composable}" else null,
            if (isExtensionFunction) "@ExtensionFunctionType" else null,
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
                append(typeArgument.render())
                if (index != typeArguments.lastIndex) append(", ")
            }
            append(">")
        }
        if (isMarkedNullable && !isStarProjection) append("?")
    }
}

fun TypeRef.renderExpanded() = expandedType?.render() ?: render()

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

fun TypeRef.getSubstitutionMap(
    baseType: TypeRef
): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()

    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (baseType.classifier.isTypeParameter) {
            substitutionMap[baseType.classifier] = thisType
            baseType.superTypes
                .map { it to thisType.expandTo(it.classifier) }
                .forEach { (baseSuperType, expandedThisType) ->
                    expandedThisType?.typeArguments?.zip(baseSuperType.typeArguments)?.forEach {
                        visitType(it.first, it.second)
                    }
                }
        } else {
            thisType.typeArguments.zip(baseType.typeArguments).forEach {
                visitType(it.first, it.second)
            }
        }
    }

    var lastSubstitutionMap: Map<ClassifierRef, TypeRef>? = null
    while (lastSubstitutionMap != substitutionMap) {
        visitType(this, baseType)
        lastSubstitutionMap = substitutionMap.toMap()
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

fun TypeRef.isAssignable(superType: TypeRef): Boolean {
    if (this == superType) return true

    if ((isStarProjection && !superType.classifier.isTypeParameter) ||
        (superType.isStarProjection && !classifier.isTypeParameter)) return true

    if (isStarProjection || superType.isStarProjection) return true

    if (qualifiers != superType.qualifiers) return false
    if (isComposableRecursive != superType.isComposableRecursive) return false

    if (superType.classifier.isTypeParameter) {
        return superType.classifier.superTypes.all { upperBound ->
            isSubTypeOf(upperBound)
        }
    }

    if (classifier.fqName != superType.classifier.fqName) return false

    if (!typeArguments.zip(superType.typeArguments).all { (a, b) -> a.isAssignable(b) })
        return false

    return true
}

fun TypeRef.isSubTypeOf(superType: TypeRef): Boolean {
    if (superType.classifier.fqName.asString() == KotlinBuiltIns.FQ_NAMES.any.asString() &&
        superType.isMarkedNullable)
        return true

    if (qualifiers != superType.qualifiers) return false
    if (isComposableRecursive != superType.isComposableRecursive) return false
    if (classifier.fqName == superType.classifier.fqName) return true

    if (superType.classifier.isTypeParameter && superType.superTypes.all {
            isSubTypeOf(it)
        }
    ) return true

    return fullyExpandedType.superTypes.any { it.isSubTypeOf(superType) } ||
            (fullyExpandedType != this && fullyExpandedType.isSubTypeOf(superType))
}

val TypeRef.isComposableRecursive: Boolean
    get() = isComposable || expandedType?.isComposableRecursive == true ||
            superTypes.any { it.isComposableRecursive }

val TypeRef.fullyExpandedType: TypeRef
    get() = expandedType?.fullyExpandedType ?: this

fun TypeRef.expandTo(classifier: ClassifierRef): TypeRef? {
    if (this.classifier == classifier) return this
    return expandedType?.expandTo(classifier)
}
