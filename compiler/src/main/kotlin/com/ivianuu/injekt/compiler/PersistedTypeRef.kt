/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import org.jetbrains.kotlin.types.model.*

@Serializable data class PersistedTypeRef(
  val classifierKey: String,
  val arguments: List<PersistedTypeRef> = emptyList(),
  val isStarProjection: Boolean,
  val variance: TypeVariance,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean
)

fun TypeRef.toPersistedTypeRef(ctx: Context): PersistedTypeRef =
  PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey(ctx) ?: "",
    arguments = arguments.map { it.toPersistedTypeRef(ctx) },
    isStarProjection = isStarProjection,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )

fun PersistedTypeRef.toTypeRef(ctx: Context): TypeRef {
  if (isStarProjection) return STAR_PROJECTION_TYPE
  val classifier = classifierDescriptorForKey(classifierKey, ctx)
    .toClassifierRef(ctx)
  val arguments = if (classifier.isTag) {
    arguments
      .map { it.toTypeRef(ctx) } +
        listOfNotNull(
          if (arguments.size < classifier.typeParameters.size)
            ctx.nullableAnyType
          else null
        )
  } else arguments.map { it.toTypeRef(ctx) }
  return classifier.untaggedType.copy(
    arguments = arguments,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )
}
