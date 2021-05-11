/*
 * Copyright 2021 Manuel Wrage
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
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

data class ClassifierRef(
    val key: String,
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val isQualifier: Boolean = false,
    val descriptor: ClassifierDescriptor? = null,
    val qualifiers: List<TypeRef> = emptyList(),
    val isGivenConstraint: Boolean = false,
    val isForTypeKey: Boolean = false,
    val primaryConstructorPropertyParameters: List<Name> = emptyList(),
    val variance: TypeVariance = TypeVariance.INV
) {
    val unqualifiedType: TypeRef get() = SimpleTypeRef(
        this,
        arguments = typeParameters.map { it.defaultType },
        variance = variance
    )

    val defaultType: TypeRef get() = qualifiers.wrap(unqualifiedType)

    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && key == other.key
    override fun hashCode(): Int = key.hashCode()
}

val ClassifierRef.givenConstraintTypeParameters: List<Name>
    get() = typeParameters
        .asSequence()
        .filter { it.isGivenConstraint }
        .map { it.fqName.shortName() }
        .toList()

val ClassifierRef.forTypeKeyTypeParameters: List<Name>
    get() = typeParameters
        .asSequence()
        .filter { it.isForTypeKey }
        .map { it.fqName.shortName() }
        .toList()

fun List<TypeRef>.wrap(type: TypeRef): TypeRef = foldRight(type) { nextQualifier, acc ->
    nextQualifier.wrap(acc)
}

fun TypeRef.unwrapQualifiers(): TypeRef = if (!classifier.isQualifier) this
else arguments.last().unwrapQualifiers()

fun TypeRef.wrap(type: TypeRef): TypeRef {
    val newArguments = if (arguments.size < classifier.typeParameters.size)
        arguments + type
    else arguments.dropLast(1) + type
    return withArguments(newArguments)
}

fun ClassifierDescriptor.toClassifierRef(
    context: InjektContext,
    trace: BindingTrace?
): ClassifierRef {
    trace?.get(InjektWritableSlices.CLASSIFIER_REF_FOR_CLASSIFIER, this)?.let { return it }
    val info = if (isDeserializedDeclaration()) context.classifierInfoFor(this, trace)
    else null
    val expandedType = if (info == null) (original as? TypeAliasDescriptor)?.underlyingType
        ?.toTypeRef(context, trace) else null

    val qualifiers = info?.qualifiers?.map { it.toTypeRef(context, trace) }
        ?: getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.type.toTypeRef(context, trace) }

    val typeParameters = mutableListOf<ClassifierRef>()

    (original as? ClassifierDescriptorWithTypeParameters)
        ?.declaredTypeParameters
        ?.forEach { typeParameters += it.toClassifierRef(context, trace) }

    val isQualifier = hasAnnotation(InjektFqNames.Qualifier)

    if (isQualifier) {
        typeParameters += ClassifierRef(
            key = "${uniqueKey(context)}.\$QT",
            fqName = fqNameSafe.child("\$QT".asNameId()),
            isTypeParameter = true,
            superTypes = listOf(context.nullableAnyType),
            variance = TypeVariance.OUT
        )
    }

    return ClassifierRef(
        key = original.uniqueKey(context),
        fqName = original.fqNameSafe,
        typeParameters = typeParameters,
        superTypes = when {
            expandedType != null -> listOf(expandedType)
            isQualifier -> listOf(context.anyType)
            info != null -> info.superTypes.map { it.toTypeRef(context, trace) }
            else -> typeConstructor.supertypes.map { it.toTypeRef(context, trace) }
        },
        isTypeParameter = this is TypeParameterDescriptor,
        isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
        isQualifier = isQualifier,
        isTypeAlias = this is TypeAliasDescriptor,
        descriptor = this,
        qualifiers = qualifiers,
        isGivenConstraint = this is TypeParameterDescriptor && isGivenConstraint(context, trace),
        isForTypeKey = this is TypeParameterDescriptor && isForTypeKey(context, trace),
        primaryConstructorPropertyParameters = info
            ?.primaryConstructorPropertyParameters
            ?.map { it.asNameId() } ?: this
            .safeAs<ClassDescriptor>()
            ?.unsubstitutedPrimaryConstructor
            ?.valueParameters
            ?.asSequence()
            ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
            ?.map { it.name }
            ?.toList()
        ?: emptyList(),
        variance = (this as? TypeParameterDescriptor)?.variance?.convertVariance() ?: TypeVariance.INV
    ).also {
        trace?.record(InjektWritableSlices.CLASSIFIER_REF_FOR_CLASSIFIER, this, it)
    }
}

fun KotlinType.toTypeRef(
    context: InjektContext,
    trace: BindingTrace?,
    isStarProjection: Boolean = false,
    variance: TypeVariance = TypeVariance.INV
): TypeRef = if (isStarProjection) STAR_PROJECTION_TYPE else {
    val kotlinType = when {
        constructor.isDenotable -> this
        constructor.supertypes.isNotEmpty() -> CommonSupertypes
            .commonSupertype(constructor.supertypes)
        else -> context.module.builtIns.nullableAnyType
    }
    val key = System.identityHashCode(kotlinType)
    trace?.get(InjektWritableSlices.TYPE_REF_FOR_TYPE, key)?.let { return it }

    val rawType = KotlinTypeRef(kotlinType, isStarProjection, variance, context, trace)

    val qualifierAnnotations = getAnnotatedAnnotations(InjektFqNames.Qualifier)
    val finalType = if (qualifierAnnotations.isNotEmpty()) {
        qualifierAnnotations
            .map { it.type.toTypeRef(context, trace) }
            .map {
                it.copy(
                    arguments = it.arguments + context.nullableAnyType,
                    isMarkedNullable = rawType.isMarkedNullable,
                    isGiven = rawType.isGiven,
                    defaultOnAllErrors = rawType.defaultOnAllErrors,
                    ignoreElementsWithErrors = rawType.ignoreElementsWithErrors,
                    variance = rawType.variance
                )
            }
            .wrap(rawType)
    } else rawType

    finalType
        .also { trace?.record(InjektWritableSlices.TYPE_REF_FOR_TYPE, key, it) }
}

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val arguments: List<TypeRef>
    abstract val isMarkedComposable: Boolean
    abstract val isGiven: Boolean
    abstract val isStarProjection: Boolean
    abstract val frameworkKey: Int?
    abstract val defaultOnAllErrors: Boolean
    abstract val ignoreElementsWithErrors: Boolean
    abstract val variance: TypeVariance

    override fun toString(): String = uniqueTypeName()

    override fun equals(other: Any?) =
        other is TypeRef && other.hashCode() == hashCode()

    private var _hashCode: Int? = null

    override fun hashCode(): Int = _hashCode ?: run {
        var result = classifier.hashCode()
        result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + isMarkedComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
        result = 31 * result + frameworkKey.hashCode()
        result
    }.also { _hashCode = it }
}

class KotlinTypeRef(
    private val kotlinType: KotlinType,
    override val isStarProjection: Boolean = false,
    override val variance: TypeVariance,
    val context: InjektContext,
    val trace: BindingTrace?
) : TypeRef() {
    override val classifier: ClassifierRef get() =
        (kotlinType.getAbbreviation() ?: kotlinType)
            .constructor.declarationDescriptor!!.toClassifierRef(context, trace)
    override val isMarkedComposable: Boolean get() =
        (kotlinType.getAbbreviation() ?: kotlinType)
            .hasAnnotation(InjektFqNames.Composable)
    override val isGiven: Boolean
        get() = kotlinType.isGiven(context, trace)
    override val isMarkedNullable: Boolean
        get() = kotlinType.isMarkedNullable
    override val arguments: List<TypeRef> get() =
        (kotlinType.getAbbreviation() ?: kotlinType).arguments
            .asSequence()
            // we use the take here because an inner class also contains the type parameters
            // of it's parent class which is irrelevant for us
            .take(classifier.typeParameters.size)
            .map { it.type.toTypeRef(context, trace, it.isStarProjection, it.projectionKind.convertVariance()) }
            .toList()
    override val frameworkKey: Int?
        get() = null
    override val defaultOnAllErrors: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.DefaultOnAllErrors)
    override val ignoreElementsWithErrors: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.IgnoreElementsWithErrors)
}

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val arguments: List<TypeRef> = emptyList(),
    override val isMarkedComposable: Boolean = false,
    override val isGiven: Boolean = false,
    override val isStarProjection: Boolean = false,
    override val frameworkKey: Int? = null,
    override val defaultOnAllErrors: Boolean = false,
    override val ignoreElementsWithErrors: Boolean = false,
    override val variance: TypeVariance = TypeVariance.INV
) : TypeRef() {
    init {
        check(arguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${arguments.map { it.render() }}"
        }
    }
}

fun TypeRef.withArguments(arguments: List<TypeRef>): TypeRef =
    if (this.arguments == arguments) this
    else copy(arguments = arguments)

fun TypeRef.withNullability(isMarkedNullable: Boolean) =
    if (this.isMarkedNullable == isMarkedNullable) this
    else copy(isMarkedNullable = isMarkedNullable)

fun TypeRef.withVariance(variance: TypeVariance) =
    if (this.variance == variance) this
    else copy(variance = variance)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    arguments: List<TypeRef> = this.arguments,
    isMarkedComposable: Boolean = this.isMarkedComposable,
    isGiven: Boolean = this.isGiven,
    isStarProjection: Boolean = this.isStarProjection,
    frameworkKey: Int? = this.frameworkKey,
    defaultOnAllErrors: Boolean = this.defaultOnAllErrors,
    ignoreElementsWithErrors: Boolean = this.ignoreElementsWithErrors,
    variance: TypeVariance = this.variance
): SimpleTypeRef = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    arguments,
    isMarkedComposable,
    isGiven,
    isStarProjection,
    frameworkKey,
    defaultOnAllErrors,
    ignoreElementsWithErrors,
    variance
)

val STAR_PROJECTION_TYPE = SimpleTypeRef(
    classifier = ClassifierRef("*", StandardNames.FqNames.any.toSafe()),
    isStarProjection = true
)

fun TypeRef.anyType(action: (TypeRef) -> Boolean): Boolean =
    action(this) || arguments.any { it.anyType(action) }

fun TypeRef.anySuperType(action: (TypeRef) -> Boolean): Boolean =
    action(this) || superTypes.any { it.anySuperType(action) }

fun TypeRef.visitRecursive(
    seen: MutableSet<TypeRef> = mutableSetOf(),
    action: (TypeRef) -> Unit
) {
    if (this in seen) return
    seen += this
    action(this)
    arguments.forEach { it.visitRecursive(seen, action) }
    superTypes.forEach { it.visitRecursive(seen, action) }
}

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    if (map.isEmpty()) return this
    map[classifier]?.let { substitution ->
        val newNullability = if (isStarProjection) substitution.isMarkedNullable
        else isMarkedNullable || substitution.isMarkedNullable
        val newGiven = isGiven || substitution.isGiven
        val newVariance = if (substitution.variance != TypeVariance.INV) substitution.variance
        else variance
        return if (newNullability != substitution.isMarkedNullable ||
            newGiven != substitution.isGiven ||
            newVariance != substitution.variance) {
            substitution.copy(
                // we copy nullability to support T : Any? -> String
                isMarkedNullable = newNullability,
                // we copy given kind to support @Given C -> @Given String
                // fallback to substitution given
                isGiven = newGiven,
                variance = newVariance
            )
        } else substitution
    }

    if (arguments.isEmpty()) return this

    val newArguments = arguments.map { it.substitute(map) }
    if (arguments != newArguments)
        return copy(arguments = newArguments)

    return this
}

fun TypeRef.render(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        fun TypeRef.inner() {
            val annotations = listOfNotNull(
                if (isGiven) "@Given" else null,
                if (isMarkedComposable) "@Composable" else null,
            )

            if (annotations.isNotEmpty()) {
                annotations.forEach { annotation ->
                    append(annotation)
                    append(" ")
                }
            }
            when {
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
            frameworkKey?.let { append("[$it]") }
        }
        inner()
    }
}

fun TypeRef.uniqueTypeName(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
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
            if (index != arguments.lastIndex) append("___")
        }
        if (isMarkedNullable && !isStarProjection) append("_nullable")
    }
}

fun KotlinType.uniqueTypeName(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        append(constructor.declarationDescriptor!!.fqNameSafe.pathSegments().joinToString("_") { it.asString() })
        arguments.forEachIndexed { index, typeArgument ->
            if (index == 0) append("_")
            append(typeArgument.type.uniqueTypeName(depth + 1))
            if (index != arguments.lastIndex) append("___")
        }
        if (isMarkedNullable) append("_nullable")
    }
}

val TypeRef.typeSize: Int get() {
    var typeSize = 0
    val seen = mutableSetOf<TypeRef>()
    fun visit(type: TypeRef) {
        typeSize++
        if (type in seen) return
        seen += type
        type.arguments.forEach { visit(it) }
    }
    visit(this)
    return typeSize
}

val TypeRef.coveringSet: Set<ClassifierRef> get() {
    val classifiers = mutableSetOf<ClassifierRef>()
    val seen = mutableSetOf<TypeRef>()
    fun visit(type: TypeRef) {
        if (type in seen) return
        seen += type
        classifiers += type.classifier
        type.arguments.forEach { visit(it) }
    }
    visit(this)
    return classifiers
}

val TypeRef.typeDepth: Int get() = (arguments.maxOfOrNull { it.typeDepth } ?: 0) + 1

val TypeRef.isNullableType: Boolean get() {
    if (isMarkedNullable) return true
    for (superType in superTypes)
        if (superType.isNullableType) return true
    return false
}

val TypeRef.isComposableType: Boolean
    get() {
        if (isMarkedComposable) return true
        for (superType in superTypes)
            if (superType.isComposableType) return true
        return false
    }

val TypeRef.superTypes: List<TypeRef>
    get() {
        val substitutionMap = classifier.typeParameters
            .toMap(arguments)
        return if (substitutionMap.isEmpty()) classifier.superTypes
        else classifier.superTypes
            .map { it.substitute(substitutionMap) }
    }

val TypeRef.isFunctionType: Boolean get() =
    classifier.fqName.asString().startsWith("kotlin.Function") ||
            classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")

val TypeRef.isSuspendFunctionType: Boolean get() =
    classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")

val TypeRef.fullyExpandedType: TypeRef
    get() = if (classifier.isTypeAlias) superTypes.single().fullyExpandedType else this

val TypeRef.isFunctionTypeWithOnlyGivenParameters: Boolean
    get() {
        if (!isFunctionType) return false
        for (i in arguments.indices) {
            if (i < arguments.lastIndex && !arguments[i].isGiven)
                return false
        }

        return true
    }

fun effectiveVariance(
    declared: TypeVariance,
    useSite: TypeVariance,
    originalDeclared: TypeVariance
): TypeVariance {
    if (useSite != TypeVariance.INV) return useSite
    if (declared != TypeVariance.INV) return declared
    return originalDeclared
}
