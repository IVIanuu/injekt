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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val descriptor: ClassifierDescriptor? = null,
    val qualifiers: List<AnnotationRef> = emptyList(),
    val isGivenConstraint: Boolean = false
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()

    val defaultType: TypeRef by unsafeLazy {
        SimpleTypeRef(
            this,
            arguments = typeParameters.map { it.defaultType },
            qualifiers = qualifiers
        )
    }
}

fun KotlinType.toTypeRef(
    context: InjektContext,
    isStarProjection: Boolean = false
): TypeRef = if (isStarProjection) STAR_PROJECTION_TYPE
else KotlinTypeRef(this, isStarProjection, context)

fun ClassifierDescriptor.toClassifierRef(
    context: InjektContext
): ClassifierRef {
    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
        ?.toTypeRef(context)
    val qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
        .map { it.toAnnotationRef(context) }
    return ClassifierRef(
        fqName = original.fqNameSafe,
        typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
            ?.map { it.toClassifierRef(context) } ?: emptyList(),
        superTypes = if (expandedType != null) listOf(expandedType) else typeConstructor.supertypes
            .map { it.toTypeRef(context) },
        isTypeParameter = this is TypeParameterDescriptor,
        isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
        isTypeAlias = this is TypeAliasDescriptor,
        descriptor = this,
        qualifiers = qualifiers,
        isGivenConstraint = this is TypeParameterDescriptor && hasAnnotation(InjektFqNames.Given)
    ).let {
        if (original.isExternalDeclaration()) it.apply(
            context,
            context.classifierInfoFor(it)
        ) else it
    }
}

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val arguments: List<TypeRef>
    abstract val isMarkedComposable: Boolean
    abstract val isGiven: Boolean
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<AnnotationRef>
    abstract val frameworkKey: String?

    private val typeName by unsafeLazy { uniqueTypeName() }

    override fun toString(): String = typeName

    override fun equals(other: Any?) =
        other is TypeRef && other.typeName == typeName

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        // todo result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + arguments.hashCode()
        // todo result result = 31 * result + variance.hashCode()
        result = 31 * result + isMarkedComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
        result = 31 * result + qualifiers.hashCode()
        result = 31 * result + frameworkKey.hashCode()

        result
    }

    override fun hashCode(): Int = _hashCode

    val typeSize: Int by unsafeLazy {
        var typeSize = 0
        val seen = mutableSetOf<TypeRef>()
        fun visit(type: TypeRef) {
            typeSize++
            if (type in seen) return
            seen += type
            type.qualifiers.forEach { visit(it.type) }
            type.arguments.forEach { visit(it) }
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
            type.qualifiers.forEach { visit(it.type) }
            type.arguments.forEach { visit(it) }
        }
        visit(this)
        classifiers
    }

    val isNullableType: Boolean by unsafeLazy {
        isMarkedNullable ||
                superTypes.any { it.isNullableType }
    }

    val isComposableType: Boolean by unsafeLazy {
        isMarkedComposable ||
                superTypes.any { it.isComposableType }
    }

    val superTypes: List<TypeRef> by unsafeLazy {
        val substitutionMap = classifier.typeParameters
            .zip(arguments)
            .toMap()
        classifier.superTypes
            .map { superType ->
                superType.substitute(substitutionMap)
                    .let {
                        if (qualifiers.isNotEmpty()) it.copy(qualifiers = (qualifiers + it.qualifiers)
                            .distinctBy { it.type.classifier })
                        else it
                    }
            }
    }

    val unqualified: TypeRef by unsafeLazy {
        if (qualifiers.isEmpty() && frameworkKey == null) this
        else copy(qualifiers = emptyList(), frameworkKey = null)
    }
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val isStarProjection: Boolean = false,
    val context: InjektContext
) : TypeRef() {
    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.toClassifierRef(context)
    }
    override val isMarkedComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val isGiven: Boolean
        get() = finalType.isGiven(context) || kotlinType.isGiven(context)
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val arguments: List<TypeRef> by unsafeLazy {
        finalType.arguments
            .take(classifier.typeParameters.size)
            .map { it.type.toTypeRef(context, it.isStarProjection) }
    }
    override val qualifiers: List<AnnotationRef> by unsafeLazy {
        kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.toAnnotationRef(context) }
    }
    override val frameworkKey: String?
        get() = null
}

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val arguments: List<TypeRef> = emptyList(),
    override val isMarkedComposable: Boolean = false,
    override val isGiven: Boolean = false,
    override val isStarProjection: Boolean = false,
    override val qualifiers: List<AnnotationRef> = emptyList(),
    override val frameworkKey: String? = null
) : TypeRef() {
    init {
        check(qualifiers.distinctBy { it.type.classifier.fqName }.size == qualifiers.size) {
            "Duplicated qualifiers ${render()}"
        }
        check(arguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${arguments.map { it.render() }}"
        }
    }
}

fun TypeRef.typeWith(arguments: List<TypeRef>): TypeRef = copy(arguments = arguments)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    arguments: List<TypeRef> = this.arguments,
    isComposable: Boolean = this.isMarkedComposable,
    isGiven: Boolean = this.isGiven,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<AnnotationRef> = this.qualifiers,
    frameworkKey: String? = this.frameworkKey
): SimpleTypeRef = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    arguments,
    isComposable,
    isGiven,
    isStarProjection,
    qualifiers,
    frameworkKey
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    if (map.isEmpty()) return this
    map[classifier]?.let { substitution ->
        return substitution.copy(
            // we copy nullability to support T : Any? -> String
            isMarkedNullable = if (!isStarProjection) isMarkedNullable else substitution.isMarkedNullable,
            // we keep qualifiers to support @MyQualifier T -> @MyQualifier String
            // but we also add the substitution qualifiers to support T -> @MyQualifier String
            // in case of an overlap we replace the original qualifier with substitution qualifier
            qualifiers = (qualifiers
                .map { qualifier ->
                    substitution.qualifiers.singleOrNull {
                        it.type.classifier == qualifier.type.classifier
                    } ?: qualifier
                }
                .map { it.substitute(map) } + substitution.qualifiers
                .filter { qualifier ->
                    qualifiers.none { it.type.classifier == qualifier.type.classifier }
                }),
            // we copy given kind to support @Given C -> @Given String
            // fallback to substitution given
            isGiven = isGiven || substitution.isGiven
        )
    }

    if (arguments.isEmpty() && qualifiers.isEmpty() &&
        !classifier.isTypeParameter
    ) return this

    val substituted = copy(
        arguments = arguments.map { it.substitute(map) },
        qualifiers = qualifiers.map { it.substitute(map) }
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
            val annotations = qualifiers.map { qualifier ->
                "@${qualifier.type.render()}${
                    if (qualifier.arguments.isNotEmpty()) {
                        qualifier.arguments.toList()
                            .joinToString(prefix = "(", postfix = ")") { (argName, argValue) ->
                                "${argName}=${argValue}"
                            }
                    } else ""
                }"
            } + listOfNotNull(
                if (isMarkedComposable) "@Composable" else null,
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
            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, typeArgument ->
                    append(typeArgument.render(depth = depth + 1))
                    if (index != arguments.lastIndex) append(", ")
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
            append(it.type.classifier.fqName)
            append(it.arguments.hashCode())
            append("_")
        }
        if (isMarkedComposable) append("composable_")
        if (isStarProjection) append("star")
        else append(classifier.fqName.pathSegments().joinToString("_") { it.asString() })
        if (frameworkKey != null) {
            append("_")
            append(frameworkKey)
        }
        arguments.forEachIndexed { index, typeArgument ->
            if (index == 0) append("_")
            append(typeArgument.uniqueTypeName(depth + 1))
            if (index != arguments.lastIndex) append("_/_")
        }
    }
}

fun KotlinType.uniqueTypeName(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        append(constructor.declarationDescriptor!!.fqNameSafe.pathSegments().joinToString("_") { it.asString() })
        arguments.forEachIndexed { index, typeArgument ->
            if (index == 0) append("_")
            append(typeArgument.type.uniqueTypeName(depth + 1))
            if (index != arguments.lastIndex) append("_/_")
        }
    }
}

fun getSubstitutionMap(
    context: InjektContext,
    pairs: List<Pair<TypeRef, TypeRef>>
): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()
    val visitedTypes = mutableSetOf<TypeRef>()
    fun visitType(thisType: TypeRef, baseType: TypeRef) {
        if (thisType == baseType) return

        if (thisType in visitedTypes && baseType in visitedTypes) {
            return
        }
        visitedTypes += thisType
        visitedTypes += baseType
        if (!baseType.classifier.isTypeParameter) {
            thisType.subtypeView(baseType.classifier)
                ?.arguments?.forEachWith(baseType.arguments) { a, b -> visitType(a, b) }
            return
        }

        if (thisType.qualifiers.isNotEmpty() &&
            thisType.qualifiers.size == baseType.qualifiers.size &&
            run {
                var allMatch = true
                thisType.qualifiers.forEachWith(baseType.qualifiers) { a, b ->
                    allMatch = allMatch || a.type.classifier == b.type.classifier &&
                            a.arguments == b.arguments
                }
                allMatch
            }) {
            thisType.qualifiers.forEachWith(baseType.qualifiers) { a, b -> visitType(a.type, b.type) }
            visitType(thisType.unqualified, baseType.unqualified)
            return
        }

        if (baseType.classifier !in substitutionMap) {
            substitutionMap[baseType.classifier] = thisType
        }
    }
    pairs.forEach { visitType(it.first, it.second) }

    substitutionMap.forEach { (baseClassifier, thisType) ->
        baseClassifier.defaultType.superTypes
            .map { thisType.subtypeView(it.classifier) to it }
            .forEach { (thisBaseTypeView, baseSuperType) ->
                if (baseSuperType.classifier.isTypeParameter) {
                    val thisTypeToUse = thisBaseTypeView ?: thisType
                    if (thisTypeToUse.qualifiers.isAssignableTo(context, baseSuperType.qualifiers)) {
                        thisTypeToUse.qualifiers.forEachWith(baseSuperType.qualifiers) { a, b ->
                            visitType(a.type, b.type)
                        }
                    }
                    visitType(thisTypeToUse, baseSuperType)
                } else {
                    visitType(thisBaseTypeView ?: thisType, baseSuperType)
                }

                thisBaseTypeView?.arguments?.forEachWith(baseSuperType.arguments) { a, b ->
                    visitType(a, b)
                }

                if (thisType.qualifiers.isAssignableTo(context, baseSuperType.qualifiers)) {
                    thisType.qualifiers.forEachWith(baseSuperType.qualifiers) { a, b ->
                        visitType(a.type, b.type)
                    }
                }
                if (thisBaseTypeView?.qualifiers?.isAssignableTo(context, baseSuperType.qualifiers) == true) {
                    thisBaseTypeView.qualifiers.forEachWith(baseSuperType.qualifiers) { a, b ->
                        visitType(a.type, b.type)
                    }
                }
            }
    }

    substitutionMap.forEach { visitType(it.value, it.key.defaultType) }
    return substitutionMap
}

fun TypeRef.isAssignableTo(
    context: InjektContext,
    superType: TypeRef
): Boolean {
    if (isStarProjection || superType.isStarProjection) return true
    if (frameworkKey != superType.frameworkKey) return false
    if (superType.classifier.isTypeParameter)
        return isSubTypeOfTypeParameter(context, superType)
    if (classifier.isTypeParameter)
        return superType.isSubTypeOfTypeParameter(context, this)
    if (isSubTypeOf(context, superType)) return true
    return false
}

private fun TypeRef.isSubTypeOfTypeParameter(
    context: InjektContext,
    typeParameter: TypeRef
): Boolean {
    val superTypesAssignable = typeParameter.superTypes.all { upperBound ->
        isSubTypeOf(context, upperBound)
    }
    if (!superTypesAssignable) return false
    if (typeParameter.qualifiers.isNotEmpty() &&
        !qualifiers.isAssignableTo(context, typeParameter.qualifiers)
    ) return false
    return true
}

private fun TypeRef.isSubTypeOfSameClassifier(
    context: InjektContext,
    superType: TypeRef
): Boolean {
    if (this == superType) return true
    if (!qualifiers.isAssignableTo(context, superType.qualifiers)) return false
    if (isComposableType != superType.isComposableType) return false
    arguments.forEachWith(superType.arguments) { a, b ->
        if (!a.isAssignableTo(context, b))
            return false
    }
    return true
}

fun TypeRef.isSubTypeOf(
    context: InjektContext,
    superType: TypeRef
): Boolean {
    if (isStarProjection) return true
    if (isNullableType && !superType.isNullableType) return false
    if (frameworkKey != superType.frameworkKey) return false
    if (superType.classifier.fqName == InjektFqNames.Any)
        return superType.qualifiers.isEmpty() ||
                qualifiers.isAssignableTo(context, superType.qualifiers)
    if (classifier == superType.classifier)
        return isSubTypeOfSameClassifier(context, superType)

    val subTypeView = subtypeView(superType.classifier)
    if (subTypeView != null)
        return subTypeView.isSubTypeOfSameClassifier(context, superType)

    if (superType.classifier.isTypeParameter) {
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(context, superType.qualifiers)
        ) return false
        return superType.superTypes.all { isSubTypeOf(context, it) }
    }
    return false
}

fun List<AnnotationRef>.isAssignableTo(context: InjektContext, superQualifiers: List<AnnotationRef>): Boolean {
    if (size != superQualifiers.size) return false
    forEachWith(superQualifiers) { a, b ->
        if (!a.isAssignableTo(context, b))
            return false
    }
    return true
}

fun AnnotationRef.isAssignableTo(context: InjektContext, superQualifier: AnnotationRef): Boolean {
    if (!type.isAssignableTo(context, superQualifier.type)) return false
    return arguments == superQualifier.arguments
}

fun TypeRef.subtypeView(classifier: ClassifierRef): TypeRef? {
    if (this.classifier == classifier) return this
    for (superType in superTypes) {
        superType.subtypeView(classifier)?.let { return it }
    }
    return null
}

val TypeRef.isFunctionType: Boolean get() =
    classifier.fqName.asString().startsWith("kotlin.Function") ||
            classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")
