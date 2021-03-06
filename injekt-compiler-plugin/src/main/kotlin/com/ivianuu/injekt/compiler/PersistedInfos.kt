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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*

@Serializable data class PersistedTypeRef(
  @SerialName("0") val classifierKey: String,
  @SerialName("1") val arguments: List<PersistedTypeRef> = emptyList(),
  @SerialName("2") val isStarProjection: Boolean,
  @SerialName("3") val isMarkedNullable: Boolean,
  @SerialName("4") val isMarkedComposable: Boolean,
  @SerialName("5") val isProvide: Boolean,
  @SerialName("6") val isInject: Boolean,
  @SerialName("7") val defaultOnAllErrors: Boolean,
  @SerialName("8") val ignoreElementsWithErrors: Boolean
)

fun TypeRef.toPersistedTypeRef(@Inject context: AnalysisContext): PersistedTypeRef =
  PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey() ?: "",
    arguments = arguments.map { it.toPersistedTypeRef() },
    isStarProjection = isStarProjection,
    isMarkedNullable = isMarkedNullable,
    isMarkedComposable = isMarkedComposable,
    isProvide = isProvide,
    isInject = isInject,
    defaultOnAllErrors = defaultOnAllErrors,
    ignoreElementsWithErrors = ignoreElementsWithErrors
  )

fun PersistedTypeRef.toTypeRef(@Inject context: AnalysisContext): TypeRef {
  if (isStarProjection) return STAR_PROJECTION_TYPE
  val classifier = context.injektContext.classifierDescriptorForKey(classifierKey)
    .toClassifierRef()
  val arguments = if (classifier.isTag) {
    arguments
      .map { it.toTypeRef() } +
        listOfNotNull(
          if (arguments.size < classifier.typeParameters.size)
            context.injektContext.nullableAnyType
          else null
        )
  } else arguments.map { it.toTypeRef() }
  return classifier.untaggedType
    .copy(
      arguments = arguments,
      isMarkedNullable = isMarkedNullable,
      isMarkedComposable = isMarkedComposable,
      isProvide = isProvide,
      isInject = isInject,
      defaultOnAllErrors = defaultOnAllErrors,
      ignoreElementsWithErrors = ignoreElementsWithErrors
    )
}
