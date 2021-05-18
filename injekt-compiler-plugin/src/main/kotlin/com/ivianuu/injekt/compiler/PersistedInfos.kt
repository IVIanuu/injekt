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

import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import org.jetbrains.kotlin.resolve.*

@Serializable data class PersistedTypeRef(
  @SerialName("0") val classifierKey: String,
  @SerialName("1") val arguments: List<PersistedTypeRef> = emptyList(),
  @SerialName("2") val isStarProjection: Boolean,
  @SerialName("3") val isMarkedNullable: Boolean,
  @SerialName("4") val isMarkedComposable: Boolean,
  @SerialName("5") val isGiven: Boolean,
  @SerialName("6") val defaultOnAllErrors: Boolean,
  @SerialName("7") val ignoreElementsWithErrors: Boolean
)

fun TypeRef.toPersistedTypeRef(context: InjektContext): PersistedTypeRef = PersistedTypeRef(
  classifierKey = classifier.descriptor?.uniqueKey(context) ?: "",
  arguments = arguments.map { it.toPersistedTypeRef(context) },
  isStarProjection = isStarProjection,
  isMarkedNullable = isMarkedNullable,
  isMarkedComposable = isMarkedComposable,
  isGiven = isGiven,
  defaultOnAllErrors = defaultOnAllErrors,
  ignoreElementsWithErrors = ignoreElementsWithErrors
)

fun PersistedTypeRef.toTypeRef(context: InjektContext, trace: BindingTrace): TypeRef {
  if (isStarProjection) return STAR_PROJECTION_TYPE
  val classifier = context.classifierDescriptorForKey(classifierKey, trace)
    .toClassifierRef(context, trace)
  val arguments = if (classifier.isQualifier) {
    arguments
      .map { it.toTypeRef(context, trace) } +
        listOfNotNull(
          if (arguments.size < classifier.typeParameters.size)
            context.nullableAnyType
          else null
        )
  } else arguments.map { it.toTypeRef(context, trace) }
  return classifier.unqualifiedType
    .copy(
      arguments = arguments,
      isMarkedNullable = isMarkedNullable,
      isMarkedComposable = isMarkedComposable,
      isGiven = isGiven,
      defaultOnAllErrors = defaultOnAllErrors,
      ignoreElementsWithErrors = ignoreElementsWithErrors
    )
}

