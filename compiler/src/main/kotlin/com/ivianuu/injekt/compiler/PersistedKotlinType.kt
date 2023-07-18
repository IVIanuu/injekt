/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.isInject
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.toAnnotation
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

@Serializable data class PersistedKotlinType(
  val classifierKey: String,
  val arguments: List<PersistedTypeProjection>,
  val tags: List<PersistedKotlinType>,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean,
  val isInject: Boolean
)

fun KotlinType.toPersistedKotlinType(ctx: Context): PersistedKotlinType = PersistedKotlinType(
  classifierKey = constructor.declarationDescriptor?.uniqueKey(ctx) ?: "",
  arguments = arguments.map { it.toPersistedTypeProjection(ctx) },
  isMarkedNullable = isMarkedNullable,
  isProvide = isProvide,
  isInject = isInject,
  tags = getTags().map { it.toPersistedKotlinType(ctx) }
)

@Serializable data class PersistedTypeProjection(
  val type: PersistedKotlinType,
  val projectionKind: Variance,
  val isStarProjection: Boolean
)

fun TypeProjection.toPersistedTypeProjection(ctx: Context) = PersistedTypeProjection(
  type = type.toPersistedKotlinType(ctx),
  projectionKind = projectionKind,
  isStarProjection = isStarProjection
)

fun PersistedKotlinType.toKotlinType(ctx: Context): KotlinType {
  val classifier = classifierDescriptorForKey(classifierKey, ctx)
  val arguments = arguments.map { it.toTypeProjection(ctx) }
  return classifier.defaultType
    .replace(
      newArguments = arguments,
      newAnnotations = Annotations.create(
        buildList {
          tags.forEach { add(it.toKotlinType(ctx).toAnnotation()) }
          if (isProvide) add(
            classifierDescriptorForFqName(
              InjektFqNames.Provide,
              NoLookupLocation.FROM_BACKEND,
              ctx
            )!!.defaultType.toAnnotation()
          )
          if (isInject) add(
            classifierDescriptorForFqName(
              InjektFqNames.Inject,
              NoLookupLocation.FROM_BACKEND,
              ctx
            )!!.defaultType.toAnnotation()
          )
        }
      )
    )
}

fun PersistedTypeProjection.toTypeProjection(ctx: Context): TypeProjection {
  val type = type.toKotlinType(ctx)
  return if (isStarProjection) StarProjectionImpl(type.constructor.declarationDescriptor.cast())
  else TypeProjectionImpl(projectionKind, type)
}