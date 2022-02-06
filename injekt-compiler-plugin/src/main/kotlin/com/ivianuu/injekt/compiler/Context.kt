/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*

@Suppress("NewApi")
class Context(
  val module: ModuleDescriptor,
  val injektFqNames: InjektFqNames,
  val trace: BindingTrace?
) : TypeCheckerContext {
  fun withTrace(trace: BindingTrace?) = Context(module, injektFqNames, trace)

  override val ctx: Context get() = this

  override fun isDenotable(type: TypeRef): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toClassifierRef(ctx) }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.collection.toClassifierRef(ctx) }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.nullableNothingType.toTypeRef(ctx = ctx) }
  val anyType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.anyType.toTypeRef(ctx = ctx) }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val sourceKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(ctx.injektFqNames.sourceKey)
    )?.toClassifierRef(ctx)
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(ctx.injektFqNames.typeKey)
    )?.toClassifierRef(ctx)
  }
}
