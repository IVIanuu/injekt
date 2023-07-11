/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.types.model.TypeVariance

@Serializable data class PersistedTypeRef(
  val classifierKey: String,
  val arguments: List<PersistedTypeRef> = emptyList(),
  val isStarProjection: Boolean,
  val variance: TypeVariance,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean,
  val isInject: Boolean
)

context(Context) fun TypeRef.toPersistedTypeRef(): PersistedTypeRef =
  PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey() ?: "",
    arguments = arguments.map { it.toPersistedTypeRef() },
    isStarProjection = isStarProjection,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide,
    isInject = isInject
  )

context(Context) fun PersistedTypeRef.toTypeRef(): TypeRef {
  if (isStarProjection) return STAR_PROJECTION_TYPE
  val classifier = classifierDescriptorForKey(classifierKey)
    .toClassifierRef()
  val arguments = if (classifier.isTag) {
    arguments
      .map { it.toTypeRef() } +
        listOfNotNull(
          if (arguments.size < classifier.typeParameters.size) nullableAnyType
          else null
        )
  } else arguments.map { it.toTypeRef() }
  return classifier.untaggedType.copy(
    arguments = arguments,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide,
    isInject = isInject
  )
}
