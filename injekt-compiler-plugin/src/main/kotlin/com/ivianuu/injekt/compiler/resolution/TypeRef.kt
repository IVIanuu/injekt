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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.getTags
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.types.model.convertVariance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ClassifierRef(
  val key: String,
  val fqName: FqName,
  val typeParameters: List<ClassifierRef> = emptyList(),
  val lazySuperTypes: Lazy<List<TypeRef>> = lazyOf(emptyList()),
  val isTypeParameter: Boolean = false,
  val isObject: Boolean = false,
  val isTag: Boolean = false,
  val descriptor: ClassifierDescriptor? = null,
  val tags: List<TypeRef> = emptyList(),
  val isSpread: Boolean = false,
  val primaryConstructorPropertyParameters: List<Name> = emptyList(),
  val variance: TypeVariance = TypeVariance.INV,
  val declaresInjectables: Boolean = false
) {
  val superTypes by lazySuperTypes

  val untaggedType: TypeRef = TypeRef(
    classifier = this,
    arguments = typeParameters.map { it.defaultType },
    variance = variance
  )
  val defaultType = tags.wrap(untaggedType)

  fun copy(
    key: String = this.key,
    fqName: FqName = this.fqName,
    typeParameters: List<ClassifierRef> = this.typeParameters,
    lazySuperTypes: Lazy<List<TypeRef>> = this.lazySuperTypes,
    isTypeParameter: Boolean = this.isTypeParameter,
    isObject: Boolean = this.isObject,
    isTag: Boolean = this.isTag,
    descriptor: ClassifierDescriptor? = this.descriptor,
    tags: List<TypeRef> = this.tags,
    isSpread: Boolean = this.isSpread,
    primaryConstructorPropertyParameters: List<Name> = this.primaryConstructorPropertyParameters,
    variance: TypeVariance = this.variance,
    declaresInjectables: Boolean = this.declaresInjectables
  ) = ClassifierRef(
    key, fqName, typeParameters, lazySuperTypes, isTypeParameter, isObject, isTag,
    descriptor, tags, isSpread, primaryConstructorPropertyParameters, variance, declaresInjectables
  )

  override fun equals(other: Any?): Boolean = (other is ClassifierRef) && key == other.key
  override fun hashCode(): Int = key.hashCode()
  override fun toString(): String = key
}

fun List<TypeRef>.wrap(type: TypeRef): TypeRef = foldRight(type) { nextTag, acc ->
  nextTag.wrap(acc)
}

fun TypeRef.unwrapTags(): TypeRef = if (!classifier.isTag) this
else arguments.last().unwrapTags()

fun TypeRef.wrap(type: TypeRef): TypeRef {
  val newArguments = if (arguments.size < classifier.typeParameters.size)
    arguments + type
  else arguments.dropLast(1) + type
  return withArguments(newArguments)
}

fun ClassifierDescriptor.toClassifierRef(@Inject ctx: Context): ClassifierRef =
  trace()!!.getOrPut(InjektWritableSlices.CLASSIFIER_REF, this) {
    val info = classifierInfo()

    val typeParameters = safeAs<ClassifierDescriptorWithTypeParameters>()
      ?.declaredTypeParameters
      ?.map { it.toClassifierRef() }
      ?.toMutableList()
      ?: mutableListOf()

    val isTag = hasAnnotation(injektFqNames().tag) || fqNameSafe == injektFqNames().composable

    if (isTag) {
      typeParameters += ClassifierRef(
        key = "${uniqueKey()}.\$TT",
        fqName = fqNameSafe.child("\$TT".asNameId()),
        isTypeParameter = true,
        lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { listOf(ctx.nullableAnyType) },
        variance = TypeVariance.OUT
      )
    }

    ClassifierRef(
      key = original.uniqueKey(),
      fqName = original.fqNameSafe,
      typeParameters = typeParameters,
      lazySuperTypes = info.lazySuperTypes,
      isTypeParameter = this is TypeParameterDescriptor,
      isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
      isTag = isTag,
      descriptor = this,
      tags = info.tags,
      isSpread = info.isSpread,
      primaryConstructorPropertyParameters = info.primaryConstructorPropertyParameters
        .map { it.asNameId() },
      variance = (this as? TypeParameterDescriptor)?.variance?.convertVariance() ?: TypeVariance.INV,
      declaresInjectables = info.declaresInjectables
    )
  }

fun KotlinType.toTypeRef(
  isStarProjection: Boolean = false,
  variance: TypeVariance = TypeVariance.INV,
  @Inject ctx: Context
): TypeRef {
  return if (isStarProjection) STAR_PROJECTION_TYPE else {
    val unwrapped = getAbbreviation() ?: this
    val kotlinType = when {
      unwrapped.constructor.isDenotable -> unwrapped
      unwrapped.constructor.supertypes.isNotEmpty() -> CommonSupertypes
        .commonSupertype(unwrapped.constructor.supertypes)
      else -> return ctx.nullableAnyType
    }

    val classifier = kotlinType.constructor.declarationDescriptor!!.toClassifierRef()

    val rawType = TypeRef(
      classifier = classifier,
      isMarkedNullable = kotlinType.isMarkedNullable,
      arguments = kotlinType.arguments
        // we use take here because an inner class also contains the type parameters
        // of it's parent class which is irrelevant for us
        .take(classifier.typeParameters.size)
        .map {
          it.type.toTypeRef(
            isStarProjection = it.isStarProjection,
            variance = it.projectionKind.convertVariance()
          )
        }
        .let {
          if (classifier.isTag && it.size != classifier.typeParameters.size)
            it + ctx.nullableAnyType
          else it
        },
      isProvide = kotlinType.hasAnnotation(injektFqNames().provide),
      isInject = kotlinType.hasAnnotation(injektFqNames().inject),
      isStarProjection = false,
      frameworkKey = "",
      variance = variance,
      isError = isError
    )

    val tagAnnotations = unwrapped.getTags(injektFqNames())
    var r = if (tagAnnotations.isNotEmpty()) {
      tagAnnotations
        .map { it.type.toTypeRef() }
        .map {
          it.copy(
            arguments = it.arguments,
            isMarkedNullable = rawType.isMarkedNullable,
            isProvide = rawType.isProvide,
            variance = rawType.variance
          )
        }
        .wrap(rawType)
    } else rawType

    // expand the type
    while (r.unwrapTags().classifier.descriptor is TypeAliasDescriptor) {
      val expanded = r.unwrapTags().superTypes.single()
      r = if (r.classifier.isTag) r.wrap(expanded) else expanded
    }

    r
  }
}

class TypeRef(
  val classifier: ClassifierRef,
  val isMarkedNullable: Boolean = false,
  val arguments: List<TypeRef> = emptyList(),
  val isProvide: Boolean = false,
  val isInject: Boolean = false,
  val isStarProjection: Boolean = false,
  val frameworkKey: String = "",
  val variance: TypeVariance = TypeVariance.INV,
  val source: ClassifierRef? = null,
  val isError: Boolean = false
) {
  override fun toString(): String = renderToString()

  override fun equals(other: Any?) =
    other is TypeRef && other.hashCode() == hashCode()

  private var _hashCode: Int = 0

  init {
    check(arguments.size == classifier.typeParameters.size) {
      "Argument size mismatch ${classifier.fqName} " +
          "params: ${classifier.typeParameters.map { it.fqName }} " +
          "args: ${arguments.map { it.renderToString() }}"
    }
  }

  private var _key: TypeRefKey? = null
  val key: TypeRefKey get() {
    if (_key == null) {
      _key = TypeRefKey(classifier, arguments.map { it.key })
    }
    return _key!!
  }

  private var _superTypes: List<TypeRef>? = null
  @OptIn(ExperimentalStdlibApi::class)
  val superTypes: List<TypeRef> get() {
    if (_superTypes == null) {
      val substitutionMap = buildMap<ClassifierRef, TypeRef> {
        for ((index, parameter) in classifier.typeParameters.withIndex())
          this[parameter] = arguments[index]
      }
      _superTypes = if (substitutionMap.isEmpty()) classifier.superTypes
        .map { it.withNullability(isMarkedNullable) }
      else classifier.superTypes.map {
        it.substitute(substitutionMap)
          .withNullability(isMarkedNullable)
      }
    }
    return _superTypes!!
  }

  private var _allTypes: Set<TypeRef>? = null
  val allTypes: Set<TypeRef> get() {
    if (_allTypes == null) {
      val allTypes = mutableSetOf<TypeRef>()
      fun collect(inner: TypeRef) {
        if (!allTypes.add(inner)) return
        inner.arguments.forEach { collect(it) }
        inner.superTypes.forEach { collect(it) }
      }
      collect(this)
      _allTypes = allTypes
    }
    return _allTypes!!
  }

  private var _isNullableType: Boolean? = null
  val isNullableType: Boolean get() {
    if (_isNullableType == null) {
      fun inner(): Boolean {
        if (isMarkedNullable) return true
        for (superType in superTypes)
          if (superType.isNullableType) return true
        return false
      }
      _isNullableType = inner()
    }
    return _isNullableType!!
  }

  private val subtypeViews = mutableMapOf<ClassifierRef, TypeRef?>()
  fun subtypeView(classifier: ClassifierRef): TypeRef? = subtypeViews.getOrPut(classifier) {
    fun TypeRef.inner(): TypeRef? {
      if (this.classifier == classifier) return this
      return superTypes
        .firstNotNullOfOrNull { it.subtypeView(classifier) }
        ?.let { return it }
    }
    inner()
  }

  override fun hashCode(): Int {
    if (_hashCode == 0) {
      var result = classifier.hashCode()
      result = 31 * result + isMarkedNullable.hashCode()
      result = 31 * result + arguments.hashCode()
      result = 31 * result + isProvide.hashCode()
      result = 31 * result + isInject.hashCode()
      result = 31 * result + isStarProjection.hashCode()
      result = 31 * result + frameworkKey.hashCode()
      result = 31 * result + variance.hashCode()
      result = 31 * result + isError.hashCode()
      _hashCode = result
      return result
    }
    return _hashCode
  }
}

data class TypeRefKey(val classifier: ClassifierRef, val arguments: List<TypeRefKey>)

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
  isProvide: Boolean = this.isProvide,
  isInject: Boolean = this.isInject,
  isStarProjection: Boolean = this.isStarProjection,
  frameworkKey: String = this.frameworkKey,
  variance: TypeVariance = this.variance,
  source: ClassifierRef? = this.source,
  isError: Boolean = this.isError
) = TypeRef(
  classifier,
  isMarkedNullable,
  arguments,
  isProvide,
  isInject,
  isStarProjection,
  frameworkKey,
  variance,
  source,
  isError
)

val STAR_PROJECTION_TYPE = TypeRef(
  classifier = ClassifierRef("*", StandardNames.FqNames.any.toSafe()),
  isStarProjection = true,
)

val TypeRef.hasErrors: Boolean
  get() = isError || arguments.any { it.hasErrors }

fun TypeRef.anyType(action: (TypeRef) -> Boolean): Boolean =
  action(this) || arguments.any { it.anyType(action) }

fun TypeRef.anySuperType(action: (TypeRef) -> Boolean): Boolean =
  action(this) || superTypes.any { it.anySuperType(action) }

fun TypeRef.firstSuperTypeOrNull(action: (TypeRef) -> Boolean): TypeRef? =
  takeIf(action) ?: superTypes.firstNotNullOfOrNull { it.firstSuperTypeOrNull(action) }

fun List<ClassifierRef>.substitute(map: Map<ClassifierRef, TypeRef>): List<ClassifierRef> {
  val allNewSuperTypes = map { mutableListOf<TypeRef>() }
  val newClassifiers = mapIndexed { index, classifier ->
    classifier.copy(lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { allNewSuperTypes[index] })
  }
  val combinedMap = map + zip(newClassifiers.map { it.defaultType })
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
    val newIsProvide = isProvide || substitution.isProvide
    val newIsInject = isInject || substitution.isInject
    val newVariance = if (substitution.variance != TypeVariance.INV) substitution.variance
    else variance
    return if (newNullability != substitution.isMarkedNullable ||
      newIsProvide != substitution.isProvide ||
      newIsInject != substitution.isInject ||
      newVariance != substitution.variance
    ) {
      substitution.copy(
        // we copy nullability to support T : Any? -> String
        isMarkedNullable = newNullability,
        // we copy injectable kind to support @Provide C -> @Provide String
        // fallback to substitution injectable
        isProvide = newIsProvide,
        isInject = newIsInject,
        variance = newVariance
      )
    } else substitution
  }

  if (arguments.isEmpty()) return this

  val newArguments = arguments.map { it.substitute(map) }
  if (newArguments != arguments)
    return copy(arguments = newArguments)

  return this
}

fun TypeRef.renderToString() = buildString {
  render { append(it) }
}

fun TypeRef.render(
  depth: Int = 0,
  renderType: (TypeRef) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return
  fun TypeRef.inner() {
    if (!renderType(this)) return

    when {
      isStarProjection -> append("*")
      else -> append(classifier.fqName.asString())
    }
    if (arguments.isNotEmpty()) {
      append("<")
      arguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1, renderType, append)
        if (index != arguments.lastIndex) append(", ")
      }
      append(">")
    }
    if (isMarkedNullable && !isStarProjection) append("?")
  }
  inner()
}

val TypeRef.typeSize: Int
  get() {
    var typeSize = 0
    val seen = mutableSetOf<TypeRef>()
    fun visit(type: TypeRef) {
      typeSize++
      if (seen.add(type))
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
      if (!seen.add(type)) return
      classifiers += type.classifier
      type.arguments.forEach { visit(it) }
    }
    visit(this)
    return classifiers
  }

val TypeRef.typeDepth: Int get() = (arguments.maxOfOrNull { it.typeDepth } ?: 0) + 1

val TypeRef.isProvideFunctionType: Boolean
  get() = isProvide && isFunctionType

val TypeRef.isFunctionType: Boolean
  get() = classifier.fqName.asString().startsWith("kotlin.Function") ||
      classifier.fqName.asString().startsWith("kotlin.coroutines.SuspendFunction") ||
      (classifier.fqName == InjektFqNames.Default.composable && arguments.last().isFunctionType)

fun effectiveVariance(
  declared: TypeVariance,
  useSite: TypeVariance,
  originalDeclared: TypeVariance
): TypeVariance {
  if (declared != TypeVariance.INV) return declared
  if (useSite != TypeVariance.INV) return useSite
  return originalDeclared
}
