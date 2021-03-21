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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.forEachWith
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.toMap
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val arguments: List<TypeRef>
    abstract val isMarkedComposable: Boolean
    abstract val isGiven: Boolean
    abstract val isStarProjection: Boolean
    abstract val frameworkKey: String?

    private val typeName by unsafeLazy { uniqueTypeName() }

    override fun toString(): String = typeName

    override fun equals(other: Any?) =
        other is TypeRef && other.typeName == typeName

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + isMarkedComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
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
            type.arguments.forEach { visit(it) }
        }
        visit(this)
        classifiers
    }

    val isNullableType: Boolean by unsafeLazy {
        var isNullableType = isMarkedNullable
        if (!isNullableType) {
            forEachUniqueSuperTypeUntil {
                isNullableType = it.isMarkedNullable
                isNullableType
            }
        }
        isNullableType
    }

    val isComposableType: Boolean by unsafeLazy {
        var isComposableType = isMarkedComposable
        if (!isComposableType) {
            forEachUniqueSuperTypeUntil {
                isComposableType = it.isMarkedComposable
                isComposableType
            }
        }
        isComposableType
    }

    val superTypes: List<TypeRef> by unsafeLazy {
        val substitutionMap = classifier.typeParameters
            .toMap(arguments)
        classifier.superTypes
            .map { it.substitute(substitutionMap) }
    }
}

fun TypeRef.forEachUniqueSuperTypeUntil(action: (TypeRef) -> Boolean) {
    val seen = mutableSetOf(this)
    var complete = false
    fun visit(type: TypeRef) {
        if (complete) return
        if (type in seen) return
        seen += type
        complete = action(type)
        if (complete) return
        type.superTypes.forEach { visit(it) }
    }
    superTypes.forEach { visit(it) }
}

fun TypeRef.forEachType(action: (TypeRef) -> Unit) {
    action(this)
    arguments.forEach { it.forEachType(action) }
}

class KotlinTypeRef(
    private val kotlinType: KotlinType,
    override val isStarProjection: Boolean = false,
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
    override val isMarkedNullable: Boolean get() = kotlinType.isMarkedNullable
    override val arguments: List<TypeRef> by unsafeLazy {
        (kotlinType.getAbbreviation() ?: kotlinType).arguments
            // we use the take here because an inner class also contains the type parameters
            // of it's parent class which is irrelevant for us
            .take(classifier.typeParameters.size)
            .map { it.type.toTypeRef(context, trace, it.isStarProjection) }
    }
    override val frameworkKey: String?
        get() = null
}

fun KotlinType.toTypeRef(
    context: InjektContext,
    trace: BindingTrace?,
    isStarProjection: Boolean = false
): TypeRef = if (isStarProjection) STAR_PROJECTION_TYPE
else {
    val key = System.identityHashCode(this)
    trace?.get(InjektWritableSlices.TYPE_REF_FOR_TYPE, key)?.let { return it }

    val typeWrappers = getAnnotatedAnnotations(InjektFqNames.TypeWrapper)
        .map { it.type.toTypeRef(context, trace) }
    val rawType: TypeRef = KotlinTypeRef(this, isStarProjection, context, trace)
    val result = typeWrappers.foldRight(rawType) { nextWrapper, acc ->
        nextWrapper.wrap(acc)
    }
    trace?.record(InjektWritableSlices.TYPE_REF_FOR_TYPE, key, result)
    result
}

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val arguments: List<TypeRef> = emptyList(),
    override val isMarkedComposable: Boolean = false,
    override val isGiven: Boolean = false,
    override val isStarProjection: Boolean = false,
    override val frameworkKey: String? = null
) : TypeRef() {
    init {
        check(arguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${arguments.map { it.render() }}"
        }
    }
}

fun TypeRef.typeWith(arguments: List<TypeRef>): TypeRef = copy(arguments = arguments)

fun TypeRef.wrap(superType: TypeRef): TypeRef = copy(
    classifier = classifier.copy(
        superTypes = listOf(superType)
    )
)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    arguments: List<TypeRef> = this.arguments,
    isMarkedComposable: Boolean = this.isMarkedComposable,
    isGiven: Boolean = this.isGiven,
    isStarProjection: Boolean = this.isStarProjection,
    frameworkKey: String? = this.frameworkKey
): SimpleTypeRef = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    arguments,
    isMarkedComposable,
    isGiven,
    isStarProjection,
    frameworkKey
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    if (map.isEmpty()) return this
    map[classifier]?.let { substitution ->
        val newNullability = if (!isStarProjection) isMarkedNullable else substitution.isMarkedNullable
        val newGiven = isGiven || substitution.isGiven
        return if (newNullability != substitution.isMarkedNullable ||
            newGiven != substitution.isGiven) {
            substitution.copy(
                // we copy nullability to support T : Any? -> String
                isMarkedNullable = newNullability,
                // we copy given kind to support @Given C -> @Given String
                // fallback to substitution given
                isGiven = newGiven
            )
        } else substitution
    }

    if (arguments.isEmpty() && !classifier.isTypeParameter) return this

    val substituted = copy(arguments = arguments.map { it.substitute(map) })

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
                classifier.isTypeParameter -> append(classifier.fqName.shortName())
                isStarProjection -> append("*")
                else -> append(classifier.fqName)
            }
            if (classifier.isTypeWrapper) {
                append("[:${superTypes.single().render()}]")
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
        if (classifier.isTypeWrapper) {
            append("[:${superTypes.single().uniqueTypeName()}]")
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
    if (pairs.all { it.first == it.second }) return emptyMap()
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
            val subType = thisType.subtypeView(baseType.classifier)
                ?: return
            subType.arguments.forEachWith(baseType.arguments) { a, b -> visitType(a, b) }
            return
        }

        baseType.superTypes
            .forEach { baseSuperType ->
                thisType.subtypeView(baseSuperType.classifier)
                    ?.arguments?.forEachWith(baseSuperType.arguments) { a, b -> visitType(a, b) }
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
                    visitType(thisTypeToUse, baseSuperType)
                } else {
                    visitType(thisBaseTypeView ?: thisType, baseSuperType)
                }

                thisBaseTypeView?.arguments?.forEachWith(baseSuperType.arguments) { a, b ->
                    visitType(a, b)
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
): Boolean = typeParameter.superTypes.all { upperBound ->
    isSubTypeOf(context, upperBound)
}

private fun TypeRef.isSubTypeOfSameClassifier(
    context: InjektContext,
    superType: TypeRef
): Boolean {
    if (this == superType) return true
    if (isComposableType != superType.isComposableType) return false
    arguments.forEachWith(superType.arguments) { a, b ->
        if (!a.isAssignableTo(context, b))
            return false
    }
    return true
}

private fun TypeRef.isSubTypeOfSameTypeWrapper(
    context: InjektContext,
    superType: TypeRef
): Boolean {
    if (this == superType) return true
    if (isComposableType != superType.isComposableType) return false
    if (!superTypes.single().isSubTypeOf(context, superType.superTypes.single())) return false
    return true
}

fun TypeRef.isSubTypeOf(
    context: InjektContext,
    superType: TypeRef
): Boolean {
    if (isStarProjection) return true
    if (isNullableType && !superType.isNullableType) return false
    if (superType.classifier.fqName == InjektFqNames.Any) return true
    if (classifier.fqName == superType.classifier.fqName)
        return isSubTypeOfSameClassifier(context, superType)

    val subTypeView = subtypeView(superType.classifier)
    if (subTypeView != null)
        return subTypeView.isSubTypeOfSameClassifier(context, superType)

    if (superType.classifier.isTypeParameter)
        return superType.superTypes.all { isSubTypeOf(context, it) }
    return false
}

fun TypeRef.subtypeView(classifier: ClassifierRef): TypeRef? {
    if (this.classifier == classifier) return this
    var subtypeView: TypeRef? = null
    forEachUniqueSuperTypeUntil {
        if (it.classifier == classifier) subtypeView = it
        subtypeView != null
    }
    return subtypeView
}

val TypeRef.isFunctionType: Boolean get() =
    classifier.fqName.asString().startsWith("kotlin.Function") ||
            classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")
