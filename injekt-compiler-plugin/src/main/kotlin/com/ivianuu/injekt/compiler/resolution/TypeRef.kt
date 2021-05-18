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
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class ClassifierRef(
  val key: String,
  val fqName: FqName,
  val typeParameters: List<ClassifierRef> = emptyList(),
  val lazySuperTypes: Lazy<List<TypeRef>> = lazyOf(emptyList()),
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
  val superTypes by lazySuperTypes
  val unqualifiedType: TypeRef
    get() = TypeRef(
      this,
      arguments = typeParameters.map { it.defaultType },
      variance = variance
    )

  fun copy(
    key: String = this.key,
    fqName: FqName = this.fqName,
    typeParameters: List<ClassifierRef> = this.typeParameters,
    lazySuperTypes: Lazy<List<TypeRef>> = this.lazySuperTypes,
    isTypeParameter: Boolean = this.isTypeParameter,
    isObject: Boolean = this.isObject,
    isTypeAlias: Boolean = this.isTypeAlias,
    isQualifier: Boolean = this.isQualifier,
    descriptor: ClassifierDescriptor? = this.descriptor,
    qualifiers: List<TypeRef> = this.qualifiers,
    isGivenConstraint: Boolean = this.isGivenConstraint,
    isForTypeKey: Boolean = this.isForTypeKey,
    primaryConstructorPropertyParameters: List<Name> = this.primaryConstructorPropertyParameters,
    variance: TypeVariance = this.variance
  ) = ClassifierRef(
    key, fqName, typeParameters, lazySuperTypes, isTypeParameter, isObject,
    isTypeAlias, isQualifier, descriptor, qualifiers, isGivenConstraint, isForTypeKey,
    primaryConstructorPropertyParameters, variance
  )

  val defaultType: TypeRef get() = qualifiers.wrap(unqualifiedType)

  override fun equals(other: Any?): Boolean = (other is ClassifierRef) && key == other.key
  override fun hashCode(): Int = key.hashCode()
}

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
  trace: BindingTrace
): ClassifierRef {
  trace.get(InjektWritableSlices.CLASSIFIER_REF_FOR_CLASSIFIER, this)?.let { return it }
  val info = classifierInfo(context, trace)

  val typeParameters = safeAs<ClassifierDescriptorWithTypeParameters>()
    ?.declaredTypeParameters
    ?.map { it.toClassifierRef(context, trace) }
    ?.toMutableList()

  val isQualifier = hasAnnotation(InjektFqNames.Qualifier)

  if (isQualifier) {
    typeParameters!! += ClassifierRef(
      key = "${uniqueKey(context)}.\$QT",
      fqName = fqNameSafe.child("\$QT".asNameId()),
      isTypeParameter = true,
      lazySuperTypes = unsafeLazy { listOf(context.nullableAnyType) },
      variance = TypeVariance.OUT
    )
  }

  return ClassifierRef(
    key = original.uniqueKey(context),
    fqName = original.fqNameSafe,
    typeParameters = typeParameters ?: emptyList(),
    lazySuperTypes = info.lazySuperTypes,
    isTypeParameter = this is TypeParameterDescriptor,
    isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
    isQualifier = isQualifier,
    isTypeAlias = this is TypeAliasDescriptor,
    descriptor = this,
    qualifiers = info.qualifiers,
    isGivenConstraint = info.isSpread,
    isForTypeKey = info.isForTypeKey,
    primaryConstructorPropertyParameters = info.primaryConstructorPropertyParameters
      .map { it.asNameId() },
    variance = (this as? TypeParameterDescriptor)?.variance?.convertVariance() ?: TypeVariance.INV
  ).also {
    trace.record(InjektWritableSlices.CLASSIFIER_REF_FOR_CLASSIFIER, this, it)
  }
}

fun KotlinType.toTypeRef(
  context: InjektContext,
  trace: BindingTrace,
  isStarProjection: Boolean = false,
  variance: TypeVariance = TypeVariance.INV
): TypeRef {
  return if (isStarProjection) STAR_PROJECTION_TYPE else {
    val unwrapped = getAbbreviation() ?: this
    val kotlinType = when {
      unwrapped.constructor.isDenotable -> unwrapped
      unwrapped.constructor.supertypes.isNotEmpty() -> CommonSupertypes
        .commonSupertype(unwrapped.constructor.supertypes)
      else -> null
    } ?: return context.nullableAnyType

    val classifier = kotlinType
      .constructor.declarationDescriptor!!.toClassifierRef(context, trace)

    val rawType = TypeRef(
      classifier = classifier,
      isMarkedNullable = kotlinType.isMarkedNullable,
      arguments = kotlinType.arguments
        .asSequence()
        // we use the take here because an inner class also contains the type parameters
        // of it's parent class which is irrelevant for us
        .take(classifier.typeParameters.size)
        .map {
          it.type.toTypeRef(
            context,
            trace,
            it.isStarProjection,
            it.projectionKind.convertVariance()
          )
        }
        .toMutableList()
        .also {
          if (classifier.isQualifier &&
            it.size != classifier.typeParameters.size
          )
            it += context.nullableAnyType
        },
      isMarkedComposable = kotlinType.hasAnnotation(InjektFqNames.Composable),
      isGiven = kotlinType.isGiven(context, trace),
      isStarProjection = false,
      frameworkKey = 0,
      defaultOnAllErrors = kotlinType.hasAnnotation(InjektFqNames.DefaultOnAllErrors),
      ignoreElementsWithErrors = kotlinType.hasAnnotation(InjektFqNames.IgnoreElementsWithErrors),
      variance = variance
    )

    val qualifierAnnotations = unwrapped.getAnnotatedAnnotations(InjektFqNames.Qualifier)
    if (qualifierAnnotations.isNotEmpty()) {
      qualifierAnnotations
        .map { it.type.toTypeRef(context, trace) }
        .map {
          it.copy(
            arguments = it.arguments,
            isMarkedNullable = rawType.isMarkedNullable,
            isGiven = rawType.isGiven,
            defaultOnAllErrors = rawType.defaultOnAllErrors,
            ignoreElementsWithErrors = rawType.ignoreElementsWithErrors,
            variance = rawType.variance
          )
        }
        .wrap(rawType)
    } else rawType
  }
}

class TypeRef(
  val classifier: ClassifierRef,
  val isMarkedNullable: Boolean = false,
  val arguments: List<TypeRef> = emptyList(),
  val isMarkedComposable: Boolean = false,
  val isGiven: Boolean = false,
  val isStarProjection: Boolean = false,
  val frameworkKey: Int = 0,
  val defaultOnAllErrors: Boolean = false,
  val ignoreElementsWithErrors: Boolean = false,
  val variance: TypeVariance = TypeVariance.INV
) {
  override fun toString(): String = render()

  override fun equals(other: Any?) =
    other is TypeRef && other.hashCode() == hashCode()

  private var _hashCode: Int = 0

  init {
    check(arguments.size == classifier.typeParameters.size) {
      "Argument size mismatch ${classifier.fqName} " +
          "params: ${classifier.typeParameters.map { it.fqName }} " +
          "args: ${arguments.map { it.render() }}"
    }
  }

  override fun hashCode(): Int {
    if (_hashCode == 0) {
      var result = classifier.hashCode()
      result = 31 * result + isMarkedNullable.hashCode()
      result = 31 * result + arguments.hashCode()
      result = 31 * result + isMarkedComposable.hashCode()
      result = 31 * result + isGiven.hashCode()
      result = 31 * result + isStarProjection.hashCode()
      result = 31 * result + frameworkKey.hashCode()
      result = 31 * result + defaultOnAllErrors.hashCode()
      result = 31 * result + ignoreElementsWithErrors.hashCode()
      result = 31 * result + variance.hashCode()
      _hashCode = result
    }
    return _hashCode
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
  frameworkKey: Int = this.frameworkKey,
  defaultOnAllErrors: Boolean = this.defaultOnAllErrors,
  ignoreElementsWithErrors: Boolean = this.ignoreElementsWithErrors,
  variance: TypeVariance = this.variance
) = TypeRef(
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

val STAR_PROJECTION_TYPE = TypeRef(
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

fun ClassifierRef.substitute(map: Map<ClassifierRef, TypeRef>): ClassifierRef {
  if (map.isEmpty()) return this
  return copy(
    lazySuperTypes = unsafeLazy { superTypes.map { it.substitute(map) } },
    typeParameters = typeParameters.substitute(map),
    qualifiers = qualifiers.map { it.substitute(map) }
  )
}

fun List<ClassifierRef>.substitute(map: Map<ClassifierRef, TypeRef>): List<ClassifierRef> {
  val allNewSuperTypes = map { mutableListOf<TypeRef>() }
  val newClassifiers = mapIndexed { index, classifier ->
    classifier.copy(lazySuperTypes = unsafeLazy { allNewSuperTypes[index] })
  }
  val combinedMap = map + toMap(newClassifiers.map { it.defaultType })
  for (i in indices) {
    val newSuperTypes = allNewSuperTypes[i]
    val oldClassifier = this[i]
    for (oldSuperType in oldClassifier.superTypes) {
      newSuperTypes += oldSuperType.substitute(combinedMap)
    }
  }
  return newClassifiers
}

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
  if (map.isEmpty()) return this
  map[classifier]?.let { substitution ->
    val newNullability = if (isStarProjection) substitution.isMarkedNullable
    else isMarkedNullable || substitution.isMarkedNullable
    val newGiven = isGiven || substitution.isGiven
    val newVariance = if (substitution.variance != TypeVariance.INV) substitution.variance
    else variance
    val newDefaultOnAllErrors = substitution.defaultOnAllErrors || defaultOnAllErrors
    val newIgnoreElementsWithErrors = substitution.ignoreElementsWithErrors ||
        ignoreElementsWithErrors
    return if (newNullability != substitution.isMarkedNullable ||
      newGiven != substitution.isGiven ||
      newVariance != substitution.variance ||
      newDefaultOnAllErrors != substitution.defaultOnAllErrors ||
      newIgnoreElementsWithErrors != substitution.ignoreElementsWithErrors
    ) {
      substitution.copy(
        // we copy nullability to support T : Any? -> String
        isMarkedNullable = newNullability,
        // we copy given kind to support @Given C -> @Given String
        // fallback to substitution given
        isGiven = newGiven,
        variance = newVariance,
        defaultOnAllErrors = newDefaultOnAllErrors,
        ignoreElementsWithErrors = newIgnoreElementsWithErrors
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
      if (frameworkKey != 0) append("[$frameworkKey]")
    }
    inner()
  }
}

val TypeRef.typeSize: Int
  get() {
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

val TypeRef.coveringSet: Set<ClassifierRef>
  get() {
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

val TypeRef.isNullableType: Boolean
  get() {
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

val TypeRef.isFunctionType: Boolean
  get() =
    classifier.fqName.asString().startsWith("kotlin.Function") ||
        classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction")

val TypeRef.isSuspendFunctionType: Boolean
  get() =
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
