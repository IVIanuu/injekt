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
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typesApproximation.*
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*
import java.util.stream.*

data class ClassifierRef(
    val key: String,
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val descriptor: ClassifierDescriptor? = null,
    val qualifiers: List<TypeRef> = emptyList(),
    val isGivenConstraint: Boolean = false,
    val isForTypeKey: Boolean = false,
    val primaryConstructorPropertyParameters: List<Name> = emptyList(),
    val variance: TypeVariance = TypeVariance.INV
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && key == other.key
    override fun hashCode(): Int = key.hashCode()

    val defaultType: TypeRef by unsafeLazy {
        SimpleTypeRef(
            this,
            arguments = typeParameters.map { it.defaultType },
            qualifiers = qualifiers,
            variance = variance
        )
    }
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
            .sortedQualifiers()

    return ClassifierRef(
        key = original.uniqueKey(context),
        fqName = original.fqNameSafe,
        typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
            ?.map { it.toClassifierRef(context, trace) } ?: emptyList(),
        superTypes = when {
            expandedType != null -> listOf(expandedType)
            info != null -> info.superTypes.map { it.toTypeRef(context, trace) }
            else -> typeConstructor.supertypes.map { it.toTypeRef(context, trace) }
        },
        isTypeParameter = this is TypeParameterDescriptor,
        isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
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
    val key = System.identityHashCode(this)
    trace?.get(InjektWritableSlices.TYPE_REF_FOR_TYPE, key)?.let { return it }
    KotlinTypeRef(this, isStarProjection, variance, context, trace)
        .also { trace?.record(InjektWritableSlices.TYPE_REF_FOR_TYPE, key, it) }
}

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val arguments: List<TypeRef>
    abstract val isMarkedComposable: Boolean
    abstract val isGiven: Boolean
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<TypeRef>
    abstract val frameworkKey: Int?
    abstract val defaultOnAllErrors: Boolean
    abstract val ignoreElementsWithErrors: Boolean
    abstract val variance: TypeVariance

    private val typeName by unsafeLazy { uniqueTypeName() }

    override fun toString(): String = typeName

    override fun equals(other: Any?) =
        other is TypeRef && other.typeName == typeName

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + arguments.hashCode()
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
            type.qualifiers.forEach { visit(it) }
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
            type.qualifiers.forEach { visit(it) }
            type.arguments.forEach { visit(it) }
        }
        visit(this)
        classifiers
    }

    val isNullableType: Boolean by unsafeLazy {
        if (isMarkedNullable) return@unsafeLazy true
        for (superType in superTypes)
            if (superType.isNullableType) return@unsafeLazy true
        return@unsafeLazy false
    }

    val isComposableType: Boolean by unsafeLazy {
        if (isMarkedComposable) return@unsafeLazy true
        for (superType in superTypes)
            if (superType.isComposableType) return@unsafeLazy true
        return@unsafeLazy false
    }

    val superTypes: List<TypeRef> by unsafeLazy {
        val substitutionMap = classifier.typeParameters
            .toMap(arguments)
        classifier.superTypes
            .map { superType ->
                superType.substitute(substitutionMap)
                    .let {
                        if (qualifiers.isNotEmpty()) it.copy(
                            qualifiers = (qualifiers + it.qualifiers)
                                .distinctBy { it.classifier.fqName.asString() }
                                .sortedQualifiers()
                        )
                        else it
                    }
            }
    }
}

class KotlinTypeRef(
    private val kotlinType: KotlinType,
    override val isStarProjection: Boolean = false,
    override val variance: TypeVariance,
    val context: InjektContext,
    val trace: BindingTrace?
) : TypeRef() {
    override val classifier: ClassifierRef by unsafeLazy {
        (kotlinType.getAbbreviation() ?: kotlinType)
            .constructor.declarationDescriptor!!.toClassifierRef(context, trace)
    }
    override val isMarkedComposable: Boolean by unsafeLazy {
        (kotlinType.getAbbreviation() ?: kotlinType)
            .hasAnnotation(InjektFqNames.Composable)
    }
    override val isGiven: Boolean
        get() = kotlinType.isGiven(context, trace)
    override val isMarkedNullable: Boolean
        get() = kotlinType.isMarkedNullable
    override val arguments: List<TypeRef> by unsafeLazy {
        (kotlinType.getAbbreviation() ?: kotlinType).arguments
            // we use the take here because an inner class also contains the type parameters
            // of it's parent class which is irrelevant for us
            .asSequence()
            .take(classifier.typeParameters.size)
            .map { it.type.toTypeRef(context, trace, it.isStarProjection, it.projectionKind.convertVariance()) }
            .toList()
    }
    override val qualifiers: List<TypeRef> by unsafeLazy {
        kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.type.toTypeRef(context, trace) }
            .sortedQualifiers()
    }
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
    override val qualifiers: List<TypeRef> = emptyList(),
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
        check(qualifiers == qualifiers.sortedQualifiers()) {
            "Qualifiers must be sorted"
        }
    }
}

fun TypeRef.typeWith(arguments: List<TypeRef>): TypeRef = copy(arguments = arguments)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    arguments: List<TypeRef> = this.arguments,
    isMarkedComposable: Boolean = this.isMarkedComposable,
    isGiven: Boolean = this.isGiven,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<TypeRef> = this.qualifiers,
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
    qualifiers,
    frameworkKey,
    defaultOnAllErrors,
    ignoreElementsWithErrors,
    variance
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    if (map.isEmpty()) return this
    map[classifier]?.let { substitution ->
        val newQualifiers = ((qualifiers
            .map { it.substitute(map) } + substitution.qualifiers)
            .distinctBy { it.classifier })
            .sortedQualifiers()
        val newNullability = if (isStarProjection) substitution.isMarkedNullable
        else isMarkedNullable || substitution.isMarkedNullable
        val newGiven = isGiven || substitution.isGiven
        val newVariance = variance
        return if (newQualifiers != substitution.qualifiers ||
            newNullability != substitution.isMarkedNullable ||
            newGiven != substitution.isGiven ||
            newVariance != substitution.variance) {
            substitution.copy(
                // we copy nullability to support T : Any? -> String
                isMarkedNullable = newNullability,
                // we merge qualifiers to support
                // * @MyQualifier T -> @MyQualifier String
                // * support T -> @MyQualifier String
                // * @MyQualifier T -> @MyQualifier @MyOtherQualifier String
                qualifiers = newQualifiers,
                // we copy given kind to support @Given C -> @Given String
                // fallback to substitution given
                isGiven = newGiven,
                variance = newVariance
            )
        } else substitution
    }

    if (arguments.isEmpty() && qualifiers.isEmpty() && !classifier.isTypeParameter) return this

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
    classifier = ClassifierRef("*", StandardNames.FqNames.any.toSafe()),
    isStarProjection = true
)

fun TypeRef.anyType(action: (TypeRef) -> Boolean): Boolean =
    action(this) || arguments.any { it.anyType(action) } || qualifiers.any { it.anyType(action) }

fun TypeRef.visitRecursive(
    seen: MutableSet<TypeRef> = mutableSetOf(),
    action: (TypeRef) -> Unit
) {
    if (this in seen) return
    seen += this
    action(this)
    arguments.forEach { it.visitRecursive(seen, action) }
    qualifiers.forEach { it.visitRecursive(seen, action) }
    superTypes.forEach { it.visitRecursive(seen, action) }
}

fun TypeRef.render(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        fun TypeRef.inner() {
            val annotations = qualifiers.map { "@${it.render()}" } + listOfNotNull(
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
            frameworkKey?.let { append("[$it]") }
        }
        inner()
    }
}

fun TypeRef.uniqueTypeName(depth: Int = 0): String {
    if (depth > 15) return ""
    return buildString {
        qualifiers.forEach {
            append(it.uniqueTypeName())
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

fun TypeRef.buildContext(
    staticTypeParameters: List<ClassifierRef>,
    superType: TypeRef
): TypeContext {
    val context = TypeContext()
    staticTypeParameters.forEach { context.addStaticTypeParameter(it) }
    visitRecursive {
        if (it.classifier.isTypeParameter) context.addTypeParameter(it.classifier)
    }
    superType.visitRecursive {
        if (it.classifier.isTypeParameter) context.addTypeParameter(it.classifier)
    }
    context.addConstraint(this, superType, Bound.Kind.UPPER)
    return context
}

fun TypeContext.getSubstitutionMap(): Map<ClassifierRef, TypeRef> {
    check(isOk)
    return typeParameters.mapNotNull { typeParameter ->
        val values = getValues(typeParameter)
        values.singleOrNull()
            ?.let { typeParameter to it }
    }.toMap()
}

fun TypeRef.isEqualTo(superType: TypeRef): Boolean {
    if (this == superType) return true
    if (isNullableType != superType.isNullableType) return false
    if (isComposableType != superType.isComposableType) return false
    if (qualifiers != superType.qualifiers) return false
    return isSubTypeOf(superType) && superType.isSubTypeOf(this)
}

fun TypeRef.isSubTypeOf(superType: TypeRef): Boolean {
    if (superType.isStarProjection) return true
    if (classifier.fqName == InjektFqNames.Nothing) return true
    if (isNullableType && !superType.isNullableType) return false
    if (superType.classifier.fqName == InjektFqNames.Any)
        return superType.qualifiers.isEmpty() ||
                (qualifiers.isNotEmpty() &&
                        qualifiers.areSubQualifiersOf(superType.qualifiers))

    val subTypeView = subtypeView(superType.classifier)
    if (subTypeView != null)
        return subTypeView.isSubTypeOfSameClassifier(superType)

    return false
}

private fun TypeRef.isSubTypeOfSameClassifier(superType: TypeRef): Boolean {
    if (this == superType) return true
    if (!qualifiers.areSubQualifiersOf(superType.qualifiers))
        return false
    if (isMarkedComposable != superType.isMarkedComposable) return false
    for (i in arguments.indices) {
        val argument = arguments[i]
        val parameter = superType.arguments[i]
        val originalParameter = superType.classifier.defaultType.arguments[i]
        val variance = effectiveVariance(parameter.variance, argument.variance, originalParameter.variance)
        val argumentOk = when (variance) {
            TypeVariance.IN -> parameter.isSubTypeOf(argument)
            TypeVariance.OUT -> argument.isSubTypeOf(parameter)
            TypeVariance.INV -> argument.isEqualTo(parameter)
        }

        if (!argumentOk) return false
    }
    return true
}

fun TypeRef.subtypeView(classifier: ClassifierRef): TypeRef? {
    if (this.classifier == classifier) return this
    return superTypes
        .firstNotNullResult { it.subtypeView(classifier) }
        ?.let { return it }
}

fun List<TypeRef>.areSubQualifiersOf(superQualifiers: List<TypeRef>): Boolean {
    if (size != superQualifiers.size) return false
    for (superQualifier in superQualifiers) {
        val thisQualifier = firstOrNull { it.classifier == superQualifier.classifier }
        if (thisQualifier?.isSubTypeOf(superQualifier) != true)
            return false
    }
    return true
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

fun List<TypeRef>.sortedQualifiers(): List<TypeRef> =
    sortedBy { it.classifier.fqName.asString() }

fun effectiveVariance(
    declared: TypeVariance,
    useSite: TypeVariance,
    originalDeclared: TypeVariance
): TypeVariance {
    if (useSite != TypeVariance.INV) return useSite
    if (declared != TypeVariance.INV) return declared
    return originalDeclared
}

class TypeContext {
    private val staticTypeParameters = mutableSetOf<ClassifierRef>()
    val typeParameters = mutableSetOf<ClassifierRef>()
    private val bounds = mutableMapOf<ClassifierRef, MutableList<Bound>>()
    private val usedInBounds = mutableMapOf<ClassifierRef, MutableList<Bound>>()

    val isOk: Boolean get() = errors.isEmpty() && typeParameters
        .none { getValues(it).size > 1 }

    val errors = mutableSetOf<ConstraintError>()

    fun addStaticTypeParameter(typeParameter: ClassifierRef) {
        staticTypeParameters += typeParameter
    }

    fun addTypeParameter(typeParameter: ClassifierRef) {
        if (typeParameter in staticTypeParameters) return
        typeParameters += typeParameter
        for (upperBound in typeParameter.superTypes)
            addBound(typeParameter, upperBound, Bound.Kind.UPPER)
    }

    private fun generateTypeParameterBound(
        parameterType: TypeRef,
        constrainingType: TypeRef,
        kind: Bound.Kind
    ) {
        if (!parameterType.isMarkedNullable || !constrainingType.isNullableType) {
            addBound(parameterType.classifier, constrainingType, kind)
            return
        }

        val notNullConstrainingType = constrainingType.copy(isMarkedNullable = false)

        if (kind == Bound.Kind.EQUAL || kind == Bound.Kind.LOWER)
            addBound(parameterType.classifier, notNullConstrainingType, Bound.Kind.LOWER)

        if (kind == Bound.Kind.EQUAL || kind == Bound.Kind.UPPER)
            addBound(parameterType.classifier, constrainingType, Bound.Kind.UPPER)
    }

    private fun addBound(
        typeParameter: ClassifierRef,
        constrainingType: TypeRef,
        kind: Bound.Kind
    ) {
        val newBound = Bound(typeParameter, constrainingType, kind, !constrainingType.anyType {
            it.classifier.isTypeParameter && it.classifier in typeParameters
        })
        val boundForTypeParameter = bounds.getOrPut(typeParameter) { mutableListOf() }
        if (newBound in boundForTypeParameter) return
        boundForTypeParameter += newBound

        if (!newBound.isProper) {
            for (dependentTypeParameter in getNestedTypeParameters(constrainingType)) {
                val dependentBounds = usedInBounds.getOrPut(dependentTypeParameter) { arrayListOf() }
                dependentBounds += newBound
            }
        }

        val boundsUsedIn = usedInBounds[typeParameter] ?: emptyList()
        for (index in boundsUsedIn.indices) {
            val boundUsedIn = boundsUsedIn[index]
            generateNewBound(boundUsedIn, newBound)
        }

        for (oldBoundsIndex in boundForTypeParameter.indices)
            addConstraintFromBounds(boundForTypeParameter[oldBoundsIndex], newBound)

        if (constrainingType.classifier in typeParameters) {
            addBound(constrainingType.classifier, typeParameter.defaultType, newBound.kind.reverse())
            return
        }

        getNestedTypeParameters(constrainingType).forEach { nestedTypeParameter ->
            val boundsForNestedTypeParameter = bounds[nestedTypeParameter]
                ?: return@forEach
            for (index in boundsForNestedTypeParameter.indices)
                generateNewBound(newBound, boundsForNestedTypeParameter[index])
        }
    }

    private fun addConstraintFromBounds(oldBound: Bound, newBound: Bound) {
        if (newBound == oldBound) return
        val oldType = oldBound.type
        val newType = newBound.type

        when {
            oldBound.kind.ordinal < newBound.kind.ordinal ->
                addConstraint(oldType, newType, Bound.Kind.UPPER)
            oldBound.kind.ordinal > newBound.kind.ordinal ->
                addConstraint(newType, oldType, Bound.Kind.UPPER)
            oldBound.kind == newBound.kind && oldBound.kind == Bound.Kind.EQUAL ->
                addConstraint(oldType, newType, Bound.Kind.EQUAL)
        }
    }

    fun addConstraint(
        subType: TypeRef,
        superType: TypeRef,
        kind: Bound.Kind
    ) {
        val checker = TypeChecker(object : TypeChecker.Callbacks {
                override fun assertEqualTypes(a: TypeRef, b: TypeRef): Boolean {
                    addConstraint(a, b, Bound.Kind.EQUAL)
                    return true
                }

                override fun assertSubType(subType: TypeRef, superType: TypeRef): Boolean {
                    addConstraint(subType, superType, Bound.Kind.UPPER)
                    return true
                }

                override fun noCorrespondingSuperType(
                    subType: TypeRef,
                    superType: TypeRef
                ): Boolean {
                    errors += ConstraintError(subType, superType, Bound.Kind.UPPER)
                    return false
                }
            }
        )
        when {
            subType.classifier in typeParameters -> {
                if (subType.qualifiers.isEmpty()) {
                    generateTypeParameterBound(subType, superType, kind)
                } else {
                    val subTypeWithoutQualifiers = subType.copy(qualifiers = emptyList())
                    val superTypeWithoutQualifiers = superType.copy(qualifiers = superType
                        .qualifiers
                        .filterNot { superQ ->
                            subType.qualifiers.any {
                                it.classifier == superQ.classifier
                            }
                        })

                    if (subType.qualifiers.size == superType.qualifiers.size) {
                        subType.qualifiers.forEachWith(superType.qualifiers) { subQ, superQ ->
                            addConstraint(subQ, superQ, Bound.Kind.UPPER)
                        }
                    } else {
                        errors += ConstraintError(subType, superType, kind)
                    }

                    generateTypeParameterBound(subTypeWithoutQualifiers,
                        superTypeWithoutQualifiers, kind)
                }
            }
            superType.classifier in typeParameters -> {
                if (superType.qualifiers.isEmpty()) {
                    generateTypeParameterBound(superType, subType, kind.reverse())
                } else {
                    val superTypeWithoutQualifiers = superType.copy(qualifiers = emptyList())
                    val subTypeWithoutQualifiers = subType.copy(qualifiers = subType
                        .qualifiers
                        .filterNot { subQ ->
                            superType.qualifiers.any {
                                it.classifier == subQ.classifier
                            }
                        })

                    if (superType.qualifiers.size == subType.qualifiers.size) {
                        superType.qualifiers.forEachWith(subType.qualifiers) { superQ, subQ ->
                            addConstraint(subQ, superQ, Bound.Kind.UPPER)
                        }
                    } else {
                        errors += ConstraintError(subType, superType, kind)
                    }

                    generateTypeParameterBound(superTypeWithoutQualifiers,
                        subTypeWithoutQualifiers, kind.reverse())
                }
            }
            else -> {
                val result = when (kind) {
                    Bound.Kind.UPPER -> checker.isSubTypeOf(subType, superType)
                    Bound.Kind.EQUAL -> checker.areEqualTypes(subType, superType)
                    else -> throw AssertionError()
                }
                if (!result)
                    errors += ConstraintError(subType, superType, kind)
            }
        }
    }

    fun getValues(typeParameter: ClassifierRef): Set<TypeRef> {
        val bounds = bounds[typeParameter] ?: return emptySet()
        if (bounds.isEmpty()) return emptySet()

        val values = mutableSetOf<TypeRef>()

        val exactBounds = bounds
            .filter { it.kind == Bound.Kind.EQUAL }
            .map { it.type }
        if (exactBounds.isNotEmpty()) {
            exactBounds
                .firstOrNull { candidate ->
                    exactBounds.all {
                        candidate.isSubTypeOf(it)
                    }
                }
                ?.let { return setOf(it) }
        }
        values += exactBounds

        val lowerBounds = bounds
            .filter { it.kind == Bound.Kind.LOWER }
            .map { it.type }
        if (lowerBounds.isNotEmpty()) {
            lowerBounds
                .firstOrNull { candidate ->
                    lowerBounds.all {
                        candidate.isSubTypeOf(it)
                    }
                }
                ?.let { return setOf(it) }
        }
        values += lowerBounds

        val upperBounds = bounds
            .filter { it.kind == Bound.Kind.UPPER }
            .map { it.type }
        if (upperBounds.isNotEmpty()) {
            upperBounds
                .firstOrNull { candidate ->
                    upperBounds.all {
                        candidate.isSubTypeOf(it)
                    }
                }
                ?.let { return setOf(it) }
        }
        values += upperBounds

        return values
    }

    private fun generateNewBound(bound: Bound, substitution: Bound) {
        if (bound === substitution) return
        // Let's have a bound 'T <=> My<R>', and a substitution 'R <=> Type'.
        // Here <=> means lower_bound, upper_bound or exact_bound constraint.
        // Then a new bound 'T <=> My<_/in/out Type>' can be generated.

        val substitutedType = when (substitution.kind) {
            Bound.Kind.EQUAL -> substitution.type
            Bound.Kind.UPPER -> substitution.type.copy(variance = TypeVariance.OUT)
            Bound.Kind.LOWER -> substitution.type.copy(variance = TypeVariance.IN)
        }

        val type = bound.type.substitute(mapOf(substitution.typeParameter to substitutedType))

        fun addNewBound(newConstrainingType: TypeRef, newBoundKind: Bound.Kind) {
            // We don't generate new recursive constraints
            if (bound.typeParameter in getNestedTypeParameters(newConstrainingType)) return
            addBound(bound.typeParameter, newConstrainingType, newBoundKind)
        }

        if (substitution.kind == Bound.Kind.EQUAL) {
            addNewBound(type, bound.kind)
            return
        }
        val upper = type
        val lower = type
        // todo
        // if we allow non-trivial type projections, we bump into errors like
        // "Empty intersection for types [MutableCollection<in ('Int'..'Int?')>, MutableCollection<out Any?>, MutableCollection<in Int>]"
        fun TypeRef.containsConstrainingTypeWithoutProjection() = anyType {
            it.classifier == substitution.type.classifier && it.variance == TypeVariance.INV
        }
        if (upper.containsConstrainingTypeWithoutProjection() && bound.kind != Bound.Kind.LOWER) {
            addNewBound(upper, Bound.Kind.UPPER)
        }
        if (lower.containsConstrainingTypeWithoutProjection() && bound.kind != Bound.Kind.UPPER) {
            addNewBound(lower, Bound.Kind.LOWER)
        }
    }

    private fun getNestedTypeParameters(type: TypeRef): List<ClassifierRef> {
        val nestedTypeVariables = mutableListOf<ClassifierRef>()
        type.visitRecursive {
            if (it.classifier.isTypeParameter &&
                    it.classifier in typeParameters)
                        nestedTypeVariables += it.classifier
        }
        return nestedTypeVariables
    }
}

data class ConstraintError(
    val subType: TypeRef,
    val superType: TypeRef,
    val kind: Bound.Kind
)

data class Bound(
    val typeParameter: ClassifierRef,
    val type: TypeRef,
    val kind: Kind,
    val isProper: Boolean
) {
    enum class Kind {
        LOWER, EQUAL, UPPER
    }
}

fun Bound.Kind.reverse() = when (this) {
    Bound.Kind.UPPER -> Bound.Kind.LOWER
    Bound.Kind.LOWER -> Bound.Kind.UPPER
    Bound.Kind.EQUAL -> Bound.Kind.EQUAL
}

private class TypeChecker(private val callbacks: Callbacks) {
    fun areEqualTypes(a: TypeRef, b: TypeRef): Boolean {
        if (a == b) return true
        if (a.classifier != b.classifier) return false

        if (a.isMarkedNullable != b.isMarkedNullable) return false
        if (a.isMarkedComposable != b.isMarkedComposable) return false
        if (areEqualQualifiers(a.qualifiers, b.qualifiers)) return false

        for (i in a.arguments.indices) {
            val parameter1 = a.arguments[i]
            val parameter2 = b.arguments[i]
            if (parameter1.isStarProjection &&
                    parameter2.isStarProjection) continue
            if (effectiveVariance(parameter1.variance, parameter2.variance, TypeVariance.INV) !=
                    effectiveVariance(parameter2.variance, parameter1.variance, TypeVariance.INV))
                        return false
            if (!callbacks.assertEqualTypes(parameter1, parameter2))
                return false
        }

        return true
    }

    fun isSubTypeOf(subType: TypeRef, superType: TypeRef): Boolean {
        if (superType.isStarProjection) return true
        if (subType.classifier.fqName == InjektFqNames.Nothing) return true
        if (subType.isNullableType && !superType.isNullableType) return false
        if (superType.classifier.fqName == InjektFqNames.Any)
            return superType.qualifiers.isEmpty() ||
                    (subType.qualifiers.isNotEmpty() &&
                            areSubQualifiersOf(subType.qualifiers, superType.qualifiers))

        val subTypeView = subType.subtypeView(superType.classifier)
        if (subTypeView != null)
            return isSubTypeOfSameClassifier(subTypeView, superType)
        else callbacks.noCorrespondingSuperType(subType, superType)

        return false
    }

    private fun isSubTypeOfSameClassifier(subType: TypeRef, superType: TypeRef): Boolean {
        if (subType == superType) return true
        if (!areSubQualifiersOf(subType.qualifiers, superType.qualifiers))
            return false
        if (subType.isMarkedComposable != superType.isMarkedComposable) return false
        for (i in subType.arguments.indices) {
            val argument = subType.arguments[i]
            val parameter = superType.arguments[i]
            val originalParameter = superType.classifier.defaultType.arguments[i]
            val variance = effectiveVariance(parameter.variance, argument.variance, originalParameter.variance)
            val argumentOk = when (variance) {
                TypeVariance.IN -> callbacks.assertSubType(parameter, argument)
                TypeVariance.OUT -> callbacks.assertSubType(argument, parameter)
                TypeVariance.INV -> callbacks.assertEqualTypes(argument, parameter)
            }

            if (!argumentOk) return false
        }
        return true
    }

    private fun areEqualQualifiers(
        subQualifiers: List<TypeRef>,
        superQualifiers: List<TypeRef>
    ): Boolean {
        for (superQualifier in superQualifiers) {
            val thisQualifier = subQualifiers.firstOrNull { it.classifier == superQualifier.classifier }
            if (thisQualifier == null || !callbacks.assertEqualTypes(thisQualifier, superQualifier))
                return false
        }
        return true
    }

    private fun areSubQualifiersOf(
        subQualifiers: List<TypeRef>,
        superQualifiers: List<TypeRef>
    ): Boolean {
        if (subQualifiers.size != superQualifiers.size) return false

        for (superQualifier in superQualifiers) {
            val subQualifier = subQualifiers.firstOrNull { it.classifier == superQualifier.classifier }
            if (subQualifier == null || !callbacks.assertSubType(subQualifier, superQualifier))
                return false
        }
        return true
    }

    interface Callbacks {
        fun assertSubType(subType: TypeRef, superType: TypeRef): Boolean
        fun assertEqualTypes(a: TypeRef, b: TypeRef): Boolean
        fun noCorrespondingSuperType(subType: TypeRef, superType: TypeRef): Boolean
    }
}
