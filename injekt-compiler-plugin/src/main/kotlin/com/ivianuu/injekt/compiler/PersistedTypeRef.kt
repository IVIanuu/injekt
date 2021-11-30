/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
import kotlinx.serialization.*

@Serializable data class PersistedTypeRef(
  val classifierKey: String,
  val arguments: List<PersistedTypeRef> = emptyList(),
  val isStarProjection: Boolean,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean,
  val isInject: Boolean
)

fun TypeRef.toPersistedTypeRef(@Inject ctx: Context): PersistedTypeRef =
  PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey() ?: "",
    arguments = arguments.map { it.toPersistedTypeRef() },
    isStarProjection = isStarProjection,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide,
    isInject = isInject
  )

fun PersistedTypeRef.toTypeRef(@Inject ctx: Context): TypeRef {
  if (isStarProjection) return STAR_PROJECTION_TYPE
  val classifier = classifierDescriptorForKey(classifierKey)
    .toClassifierRef()
  val arguments = if (classifier.isTag) {
    arguments
      .map { it.toTypeRef() } +
        listOfNotNull(
          if (arguments.size < classifier.typeParameters.size)
            ctx.nullableAnyType
          else null
        )
  } else arguments.map { it.toTypeRef() }
  return classifier.untaggedType.copy(
    arguments = arguments,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide,
    isInject = isInject
  )
}
