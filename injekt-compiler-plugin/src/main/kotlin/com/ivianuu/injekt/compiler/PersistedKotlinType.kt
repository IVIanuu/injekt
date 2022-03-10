/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

@Serializable data class PersistedKotlinType(
  val classifierKey: String,
  val arguments: List<PersistedKotlinType> = emptyList(),
  val isStarProjection: Boolean,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean,
  val isInject: Boolean,
  val variance: Variance
)

fun KotlinType.toPersistedKotlinType(ctx: Context) =
  asTypeProjection().toPersistedKotlinType(ctx)

fun TypeProjection.toPersistedKotlinType(ctx: Context): PersistedKotlinType =
  PersistedKotlinType(
    classifierKey = type.constructor.declarationDescriptor?.uniqueKey(ctx) ?: "",
    arguments = type.arguments.map { it.toPersistedKotlinType(ctx) },
    isStarProjection = isStarProjection,
    isMarkedNullable = type.isMarkedNullable,
    isProvide = type.hasAnnotation(InjektFqNames.Provide),
    isInject = type.hasAnnotation(InjektFqNames.Inject),
    variance = projectionKind
  )

fun PersistedKotlinType.toKotlinType(ctx: Context): TypeProjection {
  if (isStarProjection) return StarProjectionImpl(ctx.module.builtIns.collection.declaredTypeParameters[0])
  val classifier = classifierDescriptorForKey(classifierKey, ctx)
  val baseType = if (classifier.isTag) {
    classifier.defaultType
      .replace(arguments.dropLast(1).map { it.toKotlinType(ctx) })
      .wrap(arguments.last().toKotlinType(ctx).type)
  } else classifier.defaultType.replace(arguments.map { it.toKotlinType(ctx) })
  return baseType
    .replace(
      newAnnotations = Annotations.create(
        buildList {
          if (isProvide)
            this += AnnotationDescriptorImpl(
              ctx.module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.Provide))!!.defaultType,
              emptyMap(),
              SourceElement.NO_SOURCE
            )
          if (isInject)
            this += AnnotationDescriptorImpl(
              ctx.module.findClassAcrossModuleDependencies(ClassId.topLevel(InjektFqNames.Inject))!!.defaultType,
              emptyMap(),
              SourceElement.NO_SOURCE
            )
        }
      )
    )
    .let { if (isMarkedNullable) it.makeNullable() else it }
    .asTypeProjection()
}
