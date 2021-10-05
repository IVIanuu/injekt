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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

val KotlinType.allTypes: Set<KotlinType> get() {
  val allTypes = mutableSetOf<KotlinType>()
  fun collect(inner: KotlinType) {
    if (inner in allTypes) return
    allTypes += inner
    inner.arguments.forEach { collect(it.type) }
    inner.constructor.supertypes.forEach { collect(it) }
  }
  collect(this)
  return allTypes
}

fun KotlinType.anyType(action: (KotlinType) -> Boolean): Boolean =
  action(this) || arguments.any { it.type.anyType(action) }

fun KotlinType.anySuperType(action: (KotlinType) -> Boolean): Boolean =
  action(this) || constructor.supertypes.any { it.anySuperType(action) }

fun KotlinType.firstSuperTypeOrNull(action: (KotlinType) -> Boolean): KotlinType? =
  takeIf(action) ?: constructor.supertypes.firstNotNullOfOrNull { it.firstSuperTypeOrNull(action) }

fun TypeConstructor.substitute(map: Map<ClassifierDescriptor, KotlinType>): TypeConstructor {
  if (map.isEmpty()) return this
  /*
  return copy(
    lazySuperTypes = lazy { superTypes.map { it.substitute(map) } },
    typeParameters = typeParameters.substitute(map),
    tags = tags.map { it.substitute(map) }
  )*/
  TODO()
}

fun List<TypeConstructor>.substitute(map: Map<TypeConstructor, KotlinType>): List<TypeConstructor> {
  /*val allNewSuperTypes = map { mutableListOf<TypeRef>() }
  val newClassifiers = mapIndexed { index, classifier ->
    classifier.copy(lazySuperTypes = lazy { allNewSuperTypes[index] })
  }
  val combinedMap = map + toMap(newClassifiers.map { it.defaultType })
  for (i in indices) {
    val newSuperTypes = allNewSuperTypes[i]
    val oldClassifier = this[i]
    for (oldSuperType in oldClassifier.superTypes) {
      newSuperTypes += oldSuperType.substitute(combinedMap)
    }
  }
  return newClassifiers*/
  return this
}

fun KotlinType.substitute(map: Map<TypeConstructor, KotlinType>): KotlinType {
  return this
  /*if (map.isEmpty()) return this
  map[classifier]?.let { substitution ->
    val newNullability = if (isStarProjection) substitution.isMarkedNullable
    else isMarkedNullable || substitution.isMarkedNullable
    val newIsProvide = isProvide || substitution.isProvide
    val newIsInject = isInject || substitution.isInject
    val newVariance = if (substitution.variance != TypeVariance.INV) substitution.variance
    else variance
    val newDefaultOnAllErrors = substitution.defaultOnAllErrors || defaultOnAllErrors
    val newIgnoreElementsWithErrors = substitution.ignoreElementsWithErrors ||
        ignoreElementsWithErrors
    return if (newNullability != substitution.isMarkedNullable ||
      newIsProvide != substitution.isProvide ||
      newIsInject != substitution.isInject ||
      newVariance != substitution.variance ||
      newDefaultOnAllErrors != substitution.defaultOnAllErrors ||
      newIgnoreElementsWithErrors != substitution.ignoreElementsWithErrors
    ) {
      substitution.copy(
        // we copy nullability to support T : Any? -> String
        isMarkedNullable = newNullability,
        // we copy injectable kind to support @Provide C -> @Provide String
        // fallback to substitution injectable
        isProvide = newIsProvide,
        isInject = newIsInject,
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

  return this*/
}

fun KotlinType.renderToString() = asTypeProjection().renderToString()

fun TypeProjection.renderToString() = buildString {
  render { append(it) }
}

fun TypeProjection.render(
  depth: Int = 0,
  renderType: (TypeProjection) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return
  /*fun TypeProjection.inner() {
    if (!renderType(this)) return

    if (type.isMarkedComposable) append("@Composable ")

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
    if (isMarkedNullable && !isst) append("?")
  }
  asTypeProjection().inner()*/
}

fun KotlinType.renderKotlinLikeToString() = buildString {
  //renderKotlinLike { append(it) }
}

fun KotlinType.renderKotlinLike(append: (String) -> Unit) =
  asTypeProjection().renderKotlinLike(append = append)

fun TypeProjection.renderKotlinLike(depth: Int = 0, append: (String) -> Unit) {
  if (depth > 15) return
  fun TypeProjection.inner() {
    if (type.isMarkedComposable) append("@Composable ")

    if (type.constructor.declarationDescriptor?.hasAnnotation(InjektFqNames.Tag) == true) append("@")

    when {
      isStarProjection -> append("*")
      else -> append(type.constructor.declarationDescriptor!!.fqNameSafe.shortName().asString())
    }

    val argumentsToRender = /*if (classifier.isTag) arguments.dropLast(1) else*/ type.arguments
    if (argumentsToRender.isNotEmpty()) {
      append("<")
      argumentsToRender.forEachIndexed { index, typeArgument ->
        typeArgument.renderKotlinLike(depth = depth + 1, append)
        if (index != argumentsToRender.lastIndex) append(", ")
      }
      append(">")
    }

    /*if (classifier.isTag) {
      append(" ")
      arguments.last().renderKotlinLike(depth = depth + 1, append)
    }*/

    if (type.isMarkedNullable && !isStarProjection) append("?")
  }

  inner()
}

inline val KotlinType.isProvide: Boolean
  get() = hasAnnotation(InjektFqNames.Provide)

inline val KotlinType.isInject: Boolean
  get() = hasAnnotation(InjektFqNames.Inject)

inline val KotlinType.defaultOnAllErrors: Boolean
  get() = hasAnnotation(InjektFqNames.DefaultOnAllErrors)

inline val KotlinType.ignoreElementsWithErrors: Boolean
  get() = hasAnnotation(InjektFqNames.IgnoreElementsWithErrors)

val KotlinType.typeSize: Int
  get() {
    var typeSize = 0
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      typeSize++
      if (type in seen) return
      seen += type
      type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return typeSize
  }

val KotlinType.coveringSet: Set<ClassifierDescriptor>
  get() {
    val classifiers = mutableSetOf<ClassifierDescriptor>()
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      if (type in seen) return
      seen += type
      type.constructor.declarationDescriptor
        ?.let { classifiers += it }
      type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return classifiers
  }

val KotlinType.isMarkedComposable: Boolean
  get() = hasAnnotation(InjektFqNames.Composable)

val KotlinType.typeDepth: Int get() = (arguments.maxOfOrNull { it.type.typeDepth } ?: 0) + 1

val KotlinType.isComposableType: Boolean
  get() {
    if (isMarkedComposable) return true
    for (superType in constructor.supertypes)
      if (superType.isComposableType) return true
    return false
  }

val KotlinType.isProviderFunctionType: Boolean
  get() {
    if (!isFunctionType) return false
    for (i in arguments.indices) {
      val argument = arguments[i]
      if (i < arguments.lastIndex && !argument.type.isProvide)
        return false
    }

    return true
  }

val KotlinType.isProvideFunctionType: Boolean
  get() {
    if (!isFunctionOrSuspendFunctionType) return false
    if (!isProvide)
      for (i in arguments.indices) {
        val argument = arguments[i]
        if (i < arguments.lastIndex && argument.type.isInject)
          return false
      }

    return true
  }
