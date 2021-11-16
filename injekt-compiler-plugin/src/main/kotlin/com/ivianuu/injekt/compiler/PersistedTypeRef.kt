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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.shaded_injekt.Inject
import kotlinx.serialization.Serializable

@Serializable data class PersistedTypeRef(
  val classifierKey: String,
  val arguments: List<PersistedTypeRef> = emptyList(),
  val isStarProjection: Boolean,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean,
  val isInject: Boolean,
  val scopeComponentType: PersistedTypeRef?,
  val isEager: Boolean
)

fun TypeRef.toPersistedTypeRef(@Inject ctx: Context): PersistedTypeRef =
  PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey() ?: "",
    arguments = arguments.map { it.toPersistedTypeRef() },
    isStarProjection = isStarProjection,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide,
    isInject = isInject,
    scopeComponentType = scopeComponentType?.toPersistedTypeRef(),
    isEager = isEager
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
    isInject = isInject,
    scopeComponentType = scopeComponentType?.toTypeRef(),
    isEager = isEager
  )
}
