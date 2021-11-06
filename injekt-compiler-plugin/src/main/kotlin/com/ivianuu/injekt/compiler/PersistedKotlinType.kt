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

import com.ivianuu.injekt.compiler.resolution.isEager
import com.ivianuu.injekt.compiler.resolution.isInject
import com.ivianuu.injekt.compiler.resolution.isMarkedComposable
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.makeComposable
import com.ivianuu.injekt.compiler.resolution.makeInject
import com.ivianuu.injekt.compiler.resolution.makeProvide
import com.ivianuu.injekt.compiler.resolution.makeScoped
import com.ivianuu.injekt.compiler.resolution.scopeComponentType
import com.ivianuu.injekt.compiler.resolution.tags
import com.ivianuu.shaded_injekt.Inject
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace

@Serializable data class PersistedKotlinType(
  val classifierKey: String,
  val arguments: List<PersistedTypeProjection> = emptyList(),
  val isMarkedNullable: Boolean,
  val isMarkedComposable: Boolean,
  val isProvide: Boolean,
  val isInject: Boolean,
  val tags: List<PersistedKotlinType>,
  val injectNTypes: List<PersistedKotlinType>,
  val scopeComponentType: PersistedKotlinType?,
  val isEager: Boolean
)

@Serializable data class PersistedTypeProjection(
  val isStarProjection: Boolean,
  val type: PersistedKotlinType,
  val variance: Variance
)

fun KotlinType.toPersistedType(@Inject ctx: Context): PersistedKotlinType =
  PersistedKotlinType(
    classifierKey = constructor.declarationDescriptor!!.uniqueKey(),
    arguments = arguments.map { it.toPersistedProjection() },
    isMarkedNullable = isMarkedNullable,
    isMarkedComposable = isMarkedComposable(),
    isProvide = isProvide(),
    isInject = isInject(),
    tags = tags().map { it.toPersistedType() },
    injectNTypes = injectNTypes().map { it.toPersistedType() },
    scopeComponentType = scopeComponentType()?.toPersistedType(),
    isEager = isEager()
  )

fun PersistedKotlinType.toType(@Inject ctx: Context): KotlinType {
  val classifier = classifierDescriptorForKey(classifierKey)
  return classifier.defaultType
    .replace(newArguments = classifier.typeConstructor.parameters.zip(arguments).map {
      it.second.toProjection(it.first)
    })
    .makeNullableAsSpecified(isMarkedNullable)
    .makeComposable(isMarkedComposable)
    .makeProvide(isProvide)
    .makeInject(isInject)
    .let {
      if (scopeComponentType != null) it.makeScoped(scopeComponentType.toType(), isEager)
      else it
    }
}

fun TypeProjection.toPersistedProjection(@Inject ctx: Context): PersistedTypeProjection =
  PersistedTypeProjection(
    isStarProjection = isStarProjection,
    type = type.toPersistedType(),
    variance = projectionKind
  )

fun PersistedTypeProjection.toProjection(
  parameter: TypeParameterDescriptor,
  @Inject ctx: Context
): TypeProjection {
  if (isStarProjection) return StarProjectionImpl(parameter)
  return TypeProjectionImpl(variance, type.toType())
}
