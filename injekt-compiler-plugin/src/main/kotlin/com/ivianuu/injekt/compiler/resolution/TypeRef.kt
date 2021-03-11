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
    declarationStore: DeclarationStore,
    isStarProjection: Boolean = false
): TypeRef = if (isStarProjection) STAR_PROJECTION_TYPE
else KotlinTypeRef(this, isStarProjection, declarationStore)

fun ClassifierDescriptor.toClassifierRef(
    declarationStore: DeclarationStore
): ClassifierRef = declarationStore.classifiersCache.getOrPut(original) {
    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
        ?.toTypeRef(declarationStore)
    val qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
        .map { it.toAnnotationRef(declarationStore) }
    ClassifierRef(
        fqName = original.fqNameSafe,
        typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
            ?.map { it.toClassifierRef(declarationStore) } ?: emptyList(),
        superTypes = if (expandedType != null) listOf(expandedType) else typeConstructor.supertypes
            .map { it.toTypeRef(declarationStore) },
        isTypeParameter = this is TypeParameterDescriptor,
        isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
        isTypeAlias = this is TypeAliasDescriptor,
        descriptor = this,
        qualifiers = qualifiers,
        isGivenConstraint = this is TypeParameterDescriptor && hasAnnotation(InjektFqNames.Given)
    ).let {
        if (original.isExternalDeclaration()) it.apply(
            declarationStore,
            declarationStore.classifierInfoFor(it)
        ) else it
    }
}

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val arguments: List<TypeRef>
    abstract val isComposable: Boolean
    abstract val isGiven: Boolean
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<AnnotationRef>
    abstract val constrainedGivenChain: List<FqName>
    abstract val setKey: SetKey?

    private val typeName by unsafeLazy { uniqueTypeName() }

    override fun toString(): String = typeName

    override fun equals(other: Any?) =
        other is TypeRef && other.typeName == typeName

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        // todo result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + arguments.hashCode()
        // todo result result = 31 * result + variance.hashCode()
        result = 31 * result + isComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
        result = 31 * result + qualifiers.hashCode()
        result = 31 * result + constrainedGivenChain.hashCode()
        result = 31 * result + setKey.hashCode()

        result
    }

    override fun hashCode(): Int = _hashCode

    val thisAndAllSuperTypes: Set<TypeRef> by unsafeLazy {
        val types = mutableSetOf<TypeRef>()
        fun visit(type: TypeRef) {
            if (type in types) return
            types += type
            type.superTypes.forEach { visit(it) }
        }
        visit(this)
        types
    }

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

    val isComposableType: Boolean by unsafeLazy {
        thisAndAllSuperTypes.any { it.isComposable }
    }

    val superTypes: List<TypeRef> by unsafeLazy {
        val substitutionMap = classifier.typeParameters
            .zip(arguments)
            .toMap()
        classifier.superTypes
            .map { it.substitute(substitutionMap) }
    }

    val unqualified: TypeRef by unsafeLazy {
        if (qualifiers.isEmpty() && constrainedGivenChain.isEmpty() && setKey == null) this
        else copy(qualifiers = emptyList(), constrainedGivenChain = emptyList(), setKey = null)
    }
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val isStarProjection: Boolean = false,
    val declarationStore: DeclarationStore
) : TypeRef() {
    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.toClassifierRef(declarationStore)
    }
    override val isComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val isGiven: Boolean
        get() = finalType.isGiven(declarationStore) || kotlinType.isGiven(declarationStore)
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val arguments: List<TypeRef> by unsafeLazy {
        finalType.arguments
            .take(classifier.typeParameters.size)
            .map { it.type.toTypeRef(declarationStore, it.isStarProjection) }
    }
    override val qualifiers: List<AnnotationRef> by unsafeLazy {
        kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.toAnnotationRef(declarationStore) }
    }
    override val constrainedGivenChain: List<FqName> get() = emptyList()
    override val setKey: SetKey? get() = null
}

data class SetKey(
    val type: TypeRef,
    val callable: CallableRef
)

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val arguments: List<TypeRef> = emptyList(),
    override val isComposable: Boolean = false,
    override val isGiven: Boolean = false,
    override val isStarProjection: Boolean = false,
    override val qualifiers: List<AnnotationRef> = emptyList(),
    override val constrainedGivenChain: List<FqName> = emptyList(),
    override val setKey: SetKey? = null
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
    isComposable: Boolean = this.isComposable,
    isGiven: Boolean = this.isGiven,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<AnnotationRef> = this.qualifiers,
    constrainedGivenChain: List<FqName> = this.constrainedGivenChain,
    setKey: SetKey? = this.setKey
): SimpleTypeRef = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    arguments,
    isComposable,
    isGiven,
    isStarProjection,
    qualifiers,
    constrainedGivenChain,
    setKey
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
            val annotations = qualifiers.map {
                "@${it.type.render()}(${
                    it.arguments.toList().joinToString { "${it.first}=${it.second}" }
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
        if (isComposable) append("composable_")
        if (isStarProjection) append("star")
        else append(classifier.fqName.pathSegments().joinToString("_") { it.asString() })
        if (constrainedGivenChain.isNotEmpty()) {
            append(constrainedGivenChain.joinToString("_", prefix = "_", postfix = "_"))
        }
        if (setKey != null) append("_${setKey.hashCode()}_")
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
    declarationStore: DeclarationStore,
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
            thisType.arguments.forEachWith(baseType.arguments) { a, b -> visitType(a, b) }
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
            visitType(thisType.unqualified, baseType.unqualified)
            thisType.qualifiers.forEachWith(baseType.qualifiers) { a, b -> visitType(a.type, b.type) }
            return
        }

        if (baseType.classifier !in substitutionMap) {
            substitutionMap[baseType.classifier] = thisType
        }
    }
    pairs.forEach { visitType(it.first, it.second) }

    substitutionMap.forEach { (baseClassifier, thisType) ->
        baseClassifier.defaultType.superTypes
            .map { thisType.subtypeView(declarationStore, it.classifier) to it }
            .forEach { (thisBaseTypeView, baseSuperType) ->
                if (baseSuperType.classifier.isTypeParameter) {
                    val thisTypeToUse = thisBaseTypeView ?: thisType
                    visitType(thisTypeToUse.unqualified, baseSuperType)
                    if (thisTypeToUse.qualifiers.isAssignableTo(declarationStore, baseSuperType.qualifiers)) {
                        thisTypeToUse.qualifiers.forEachWith(baseSuperType.qualifiers) { a, b ->
                            visitType(a.type, b.type)

                        }
                    }
                } else {
                    visitType(thisBaseTypeView ?: thisType, baseSuperType)
                }

                thisBaseTypeView?.arguments?.forEachWith(baseSuperType.arguments) { a, b ->
                    visitType(a, b)
                }

                if (thisType.qualifiers.isAssignableTo(declarationStore, baseSuperType.qualifiers)) {
                    thisType.qualifiers.forEachWith(baseSuperType.qualifiers) { a, b ->
                        visitType(a.type, b.type)
                    }
                }
                if (thisBaseTypeView?.qualifiers?.isAssignableTo(declarationStore, baseSuperType.qualifiers) == true) {
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
    declarationStore: DeclarationStore,
    superType: TypeRef
): Boolean = declarationStore.isAssignableCache.getOrPut(MultiKey2(this, superType)) {
    if (isStarProjection || superType.isStarProjection) return@getOrPut true
    if (isSubTypeOf(declarationStore, superType)) return@getOrPut true
    if (superType.classifier.isTypeParameter) {
        val superTypesAssignable = superType.superTypes.all { upperBound ->
            isSubTypeOf(declarationStore, upperBound)
        }
        if (!superTypesAssignable) return@getOrPut false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(declarationStore, superType.qualifiers)
        ) return@getOrPut false
        if (constrainedGivenChain != superType.constrainedGivenChain) return@getOrPut false
        if (setKey != superType.setKey) return@getOrPut false
        return@getOrPut true
    } else if (classifier.isTypeParameter) {
        val superTypesAssignable = superTypes.all { upperBound ->
            superType.isSubTypeOf(declarationStore, upperBound)
        }
        if (!superTypesAssignable) return@getOrPut false
        if (qualifiers.isNotEmpty() &&
            !superType.qualifiers.isAssignableTo(declarationStore, qualifiers)
        ) return@getOrPut false
        if (constrainedGivenChain != superType.constrainedGivenChain) return@getOrPut false
        if (setKey != superType.setKey) return@getOrPut false
        return@getOrPut true
    }

    val subTypeView = subtypeView(declarationStore, superType.classifier)
    if (subTypeView != null &&
        subTypeView != this &&
        subTypeView.isAssignableTo(declarationStore, superType)) return@getOrPut true

    return@getOrPut false
}

fun TypeRef.isSubTypeOf(
    declarationStore: DeclarationStore,
    superType: TypeRef
): Boolean = declarationStore.isSubTypeCache.getOrPut(MultiKey2(this, superType)) {
    if (isStarProjection) return@getOrPut true
    if (classifier.fqName == superType.classifier.fqName) {
        if (isMarkedNullable && !superType.isMarkedNullable) return@getOrPut false
        if (!qualifiers.isAssignableTo(declarationStore, superType.qualifiers)) return@getOrPut false
        if (constrainedGivenChain != superType.constrainedGivenChain) return@getOrPut false
        if (setKey != superType.setKey) return@getOrPut false
        if (isComposableType != superType.isComposableType) return@getOrPut false
        arguments.forEachWith(superType.arguments) { a, b ->
            if (!a.isAssignableTo(declarationStore, b))
                return@getOrPut false
        }
        return@getOrPut true
    }
    if (superType.classifier.fqName == InjektFqNames.Any) {
        if (isMarkedNullable && !superType.isMarkedNullable) return@getOrPut false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(declarationStore, superType.qualifiers)
        ) return@getOrPut false
        if (constrainedGivenChain != superType.constrainedGivenChain) return@getOrPut false
        if (setKey != superType.setKey) return@getOrPut false
        return@getOrPut true
    }
    val subTypeView = subtypeView(declarationStore, superType.classifier)
    if (subTypeView != null) {
        if (subTypeView == superType && (!subTypeView.isMarkedNullable || superType.isMarkedNullable))
            return@getOrPut true
        if (subTypeView.isMarkedNullable && !superType.isMarkedNullable) return@getOrPut false
        if (!subTypeView.qualifiers.isAssignableTo(declarationStore, superType.qualifiers)) return@getOrPut false
        if (subTypeView.constrainedGivenChain != superType.constrainedGivenChain) return@getOrPut false
        if (subTypeView.setKey != superType.setKey) return@getOrPut false
        if (isComposableType != superType.isComposableType) return@getOrPut false
        subTypeView.arguments.forEachWith(superType.arguments) { a, b ->
            if (!a.isSubTypeOf(declarationStore, b)) return@getOrPut false
        }
        return@getOrPut true
    } else if ((superType.classifier.isTypeParameter && !classifier.isTypeParameter) || (superType.classifier.isTypeAlias &&
                !classifier.isTypeAlias)) {
        if (superType.classifier.isTypeAlias &&
                thisAndAllSuperTypes.none { it.classifier == superType.classifier })
                    return@getOrPut false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(declarationStore, superType.qualifiers)
        ) return@getOrPut false
        if (constrainedGivenChain != superType.constrainedGivenChain) return@getOrPut false
        if (setKey != superType.setKey) return@getOrPut false
        if (isComposableType != superType.isComposableType) return@getOrPut false
        return@getOrPut superType.superTypes.all { upperBound ->
            // todo should do this comparison without qualifiers?
            isSubTypeOf(declarationStore, upperBound) ||
                    (superType.qualifiers.isNotEmpty() &&
                            unqualified.isSubTypeOf(declarationStore, upperBound))
        }
    }
    return@getOrPut false
}

fun List<AnnotationRef>.isAssignableTo(declarationStore: DeclarationStore, superQualifiers: List<AnnotationRef>): Boolean {
    if (size != superQualifiers.size) return false
    forEachWith(superQualifiers) { a, b ->
        if (!a.isAssignableTo(declarationStore, b))
            return false
    }
    return true
}

fun AnnotationRef.isAssignableTo(declarationStore: DeclarationStore, superQualifier: AnnotationRef): Boolean {
    if (!type.isAssignableTo(declarationStore, superQualifier.type)) return false
    return arguments == superQualifier.arguments
}

val TypeRef.fullyExpandedType: TypeRef
    get() = if (classifier.isTypeAlias) superTypes.single().fullyExpandedType else this

val KotlinType.fullyAbbreviatedType: KotlinType
    get() {
        val abbreviatedType = getAbbreviatedType()
        return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
    }

fun TypeRef.subtypeView(
    declarationStore: DeclarationStore,
    classifier: ClassifierRef
): TypeRef? = declarationStore.subTypeViewCache.getOrPut(MultiKey2(this, classifier)) {
    if (this.classifier == classifier) return@getOrPut this
    fun TypeRef.superTypeWithMatchingClassifier(): TypeRef? {
        if (this.classifier == classifier) return this
        for (superType in superTypes) {
            superType.subtypeView(declarationStore, classifier)?.let { return it }
        }
        return null
    }
    val rawSubTypeView = superTypeWithMatchingClassifier() ?: return null
    return if ((constrainedGivenChain.isNotEmpty() &&
                constrainedGivenChain != rawSubTypeView.constrainedGivenChain) ||
        (qualifiers.isNotEmpty() && qualifiers != rawSubTypeView.qualifiers) ||
        (isMarkedNullable && isMarkedNullable != rawSubTypeView.isMarkedNullable)) {
        rawSubTypeView.copy(
            constrainedGivenChain = constrainedGivenChain,
            qualifiers = qualifiers + rawSubTypeView.qualifiers,
            isMarkedNullable = isMarkedNullable
        )
    } else rawSubTypeView
}

val TypeRef.isFunctionType: Boolean get() =
    classifier.fqName.asString().startsWith("kotlin.Function") ||
            classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")
