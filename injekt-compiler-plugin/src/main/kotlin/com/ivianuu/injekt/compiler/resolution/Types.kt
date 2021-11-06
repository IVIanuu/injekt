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
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.classifierDescriptorForFqName
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectNTypes
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.module
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

private val KotlinType.fullyExpandedType: KotlinType
  get() = getAbbreviatedType()?.expandedType?.fullyExpandedType ?: this

fun TypeProjection.prepareForInjekt(
  @Inject ctx: Context
): TypeProjection = if (isStarProjection) this
else TypeProjectionImpl(projectionKind, type.prepareForInjekt())

fun KotlinType.prepareForInjekt(
  @Inject ctx: Context
): KotlinType {
  val unwrapped = getAbbreviation() ?: this
  var kotlinType = when {
    unwrapped.constructor.isDenotable -> unwrapped
    unwrapped.constructor.supertypes.isNotEmpty() -> CommonSupertypes
      .commonSupertype(unwrapped.constructor.supertypes)
    else -> return module().builtIns.nullableAnyType
  }

  while (kotlinType is AbbreviatedType) {
    kotlinType = kotlinType.expandedType
  }

  return kotlinType
}

fun KotlinType.withFrameworkKey(frameworkKey: Int, @Inject ctx: Context): KotlinType =
  addOrRemoveAnnotation(frameworkKey != 0, injektFqNames().frameworkKey) {
    createAnnotation(injektFqNames().frameworkKey, mapOf("value".asNameId() to IntValue(frameworkKey)))
  }

fun KotlinType.makeProvide(isProvide: Boolean, @Inject ctx: Context): KotlinType =
  addOrRemoveAnnotation(isProvide, injektFqNames().provide)

fun KotlinType.makeInject(isInject: Boolean, @Inject ctx: Context): KotlinType =
  addOrRemoveAnnotation(isInject, injektFqNames().inject)

fun KotlinType.makeComposable(isComposable: Boolean, @Inject ctx: Context): KotlinType =
  addOrRemoveAnnotation(isComposable, injektFqNames().composable)

fun KotlinType.makeScoped(componentType: KotlinType, isEager: Boolean, @Inject ctx: Context): KotlinType =
  addOrRemoveAnnotation(true, injektFqNames().scoped) {
    createAnnotation(
      classifierDescriptorForFqName(injektFqNames().scoped, NoLookupLocation.FROM_BACKEND)!!
        .defaultType
        .replace(newArguments = listOf(componentType.asTypeProjection())),
      mapOf("eager".asNameId() to BooleanValue(isEager))
    )
  }

inline fun KotlinType.addOrRemoveAnnotation(
  condition: Boolean,
  fqName: FqName,
  force: Boolean = false,
  create: () -> AnnotationDescriptor = { createAnnotation(fqName, emptyMap()) }
): KotlinType {
  val newAnnotations = annotations.addOrRemoveAnnotation(condition, fqName, force, create)
  return if (annotations != newAnnotations) replaceAnnotations(newAnnotations)
  else this
}

inline fun Annotations.addOrRemoveAnnotation(
  condition: Boolean,
  fqName: FqName,
  force: Boolean = false,
  create: () -> AnnotationDescriptor = { createAnnotation(fqName, emptyMap()) }
): Annotations = if (condition) {
  if (!force && hasAnnotation(fqName)) this
  else Annotations.create(filter { it.fqName != fqName } + create())
} else {
  Annotations.create(filter { it.fqName != fqName })
}

fun createAnnotation(
  fqName: FqName,
  arguments: Map<Name, ConstantValue<*>>,
  @Inject ctx: Context
): AnnotationDescriptor = createAnnotation(
  classifierDescriptorForFqName(fqName, NoLookupLocation.FROM_BACKEND)!!.defaultType,
  arguments
)

fun createAnnotation(
  type: KotlinType,
  arguments: Map<Name, ConstantValue<*>>,
  @Inject ctx: Context
): AnnotationDescriptor = AnnotationDescriptorImpl(
  type,
  arguments,
  SourceElement.NO_SOURCE
)

fun KotlinType.renderToString() = buildString {
  asTypeProjection().render { append(it) }
}

fun TypeProjection.render(
  depth: Int = 0,
  renderType: (TypeProjection) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return
  fun TypeProjection.inner() {
    if (!renderType(this)) return

    if (type.isMarkedComposable()) append("@Composable ")

    val injectNTypes = type.injectNTypes()
    if (injectNTypes.isNotEmpty()) {
      append("@Inject<")
      injectNTypes.forEachIndexed { index, injectNType ->
        injectNType.asTypeProjection().render(depth = depth + 1, renderType, append)
        if (index != injectNTypes.size - 1) append(", ")
      }
      append("> ")
    }

    when {
      isStarProjection -> append("*")
      else -> append(type.fqNameSafe.asString())
    }
    if (type.arguments.isNotEmpty()) {
      append("<")
      type.arguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1, renderType, append)
        if (index != type.arguments.lastIndex) append(", ")
      }
      append(">")
    }
    if (type.isMarkedNullable && !isStarProjection) append("?")
  }
  type.asTypeProjection().inner()
}

fun KotlinType.tags(@Inject ctx: Context): List<KotlinType> =
  getAnnotatedAnnotations(injektFqNames().tag)
    .map { it.type }

fun KotlinType.isProvide(@Inject ctx: Context): Boolean =
  hasAnnotation(injektFqNames().provide)

fun KotlinType.isInject(@Inject ctx: Context): Boolean =
  hasAnnotation(injektFqNames().inject)

fun KotlinType.scopeComponentType(@Inject ctx: Context): KotlinType? =
  annotations.findAnnotation(injektFqNames().scoped)?.type?.arguments?.single()?.type

fun KotlinType.isEager(@Inject ctx: Context): Boolean =
  annotations.findAnnotation(injektFqNames().scoped)
    ?.argumentValue("eager")?.value == true

fun KotlinType.isMarkedComposable(@Inject ctx: Context): Boolean =
  hasAnnotation(injektFqNames().composable)

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

val KotlinType.coveringSet: Set<TypeConstructor>
  get() {
    val constructors = mutableSetOf<TypeConstructor>()
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      if (type in seen) return
      seen += type
      constructors += type.constructor
      type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return constructors
  }

val KotlinType.typeDepth: Int get() = (arguments.maxOfOrNull { it.type.typeDepth } ?: 0) + 1

fun KotlinType.isComposableTypeOrSubType(@Inject ctx: Context): Boolean {
  if (isMarkedComposable()) return true
  for (superType in constructor.supertypes)
    if (superType.isComposableTypeOrSubType()) return true
  return false
}

fun KotlinType.isProvideFunctionType(@Inject ctx: Context): Boolean =
  isProvide() && isFunctionOrSuspendFunctionType

val KotlinType.fqNameSafe: FqName
  get() = constructor.declarationDescriptor?.fqNameSafe ?: FqName.ROOT