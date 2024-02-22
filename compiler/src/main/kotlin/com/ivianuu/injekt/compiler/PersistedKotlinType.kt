/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@Serializable data class PersistedKotlinType(
  val classifierKey: String,
  val arguments: List<PersistedTypeProjection>,
  val isNullable: Boolean,
  val isProvide: Boolean
)

fun KotlinType.toPersistedKotlinType(ctx: Context): PersistedKotlinType = PersistedKotlinType(
  classifierKey = constructor.declarationDescriptor?.uniqueKey(ctx) ?: "",
  arguments = arguments.map { it.toPersistedTypeProjection(ctx) },
  isNullable = isMarkedNullable,
  isProvide = hasAnnotation(InjektFqNames.Provide)
)

@Serializable data class PersistedTypeProjection(
  val type: PersistedKotlinType,
  val projectionKind: ProjectionKind
)

fun TypeProjection.toPersistedTypeProjection(ctx: Context) = PersistedTypeProjection(
  type = type.toPersistedKotlinType(ctx),
  projectionKind = if (isStarProjection) ProjectionKind.STAR
  else when (projectionKind) {
    Variance.INVARIANT -> ProjectionKind.INVARIANT
    Variance.IN_VARIANCE -> ProjectionKind.IN
    Variance.OUT_VARIANCE -> ProjectionKind.OUT
  }
)

fun PersistedKotlinType.toKotlinType(ctx: Context): KotlinType {
  val arguments = arguments.map { it.toTypeProjection(ctx) }
  val classifier = classifierDescriptorForKey(classifierKey, ctx)
  return if (classifier.hasAnnotation(InjektFqNames.Tag))
    classifier.defaultType.wrapTag(null, arguments)
  else classifier.defaultType
    .replace(
      newArguments = arguments,
      newAnnotations = if (!isProvide) Annotations.EMPTY
      else Annotations.create(
        listOf(
          AnnotationDescriptorImpl(
            classifierDescriptorForFqName(
              InjektFqNames.Provide,
              NoLookupLocation.FROM_BACKEND,
              ctx
            )!!.defaultType,
            emptyMap(),
            SourceElement.NO_SOURCE
          )
        )
      )
    )
}

fun PersistedTypeProjection.toTypeProjection(ctx: Context): TypeProjection {
  val type = type.toKotlinType(ctx)
  return when (projectionKind) {
    ProjectionKind.STAR -> StarProjectionImpl(type.constructor.declarationDescriptor.cast())
    ProjectionKind.IN -> TypeProjectionImpl(Variance.INVARIANT, type)
    ProjectionKind.OUT -> TypeProjectionImpl(Variance.INVARIANT, type)
    ProjectionKind.INVARIANT -> TypeProjectionImpl(Variance.INVARIANT, type)
  }
}
