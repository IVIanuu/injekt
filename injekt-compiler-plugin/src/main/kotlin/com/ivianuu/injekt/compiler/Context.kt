/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.TypeCheckerContext
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingTrace

inline fun injektFqNames(@Inject ctx: Context) = ctx.injektFqNames

inline fun trace(@Inject ctx: Context) = ctx.trace

inline fun module(@Inject ctx: Context) = ctx.module

@Suppress("NewApi")
class Context(
  val module: ModuleDescriptor,
  val injektFqNames: InjektFqNames,
  val trace: BindingTrace?
) : TypeCheckerContext {
  fun withTrace(trace: BindingTrace?) = Context(module, injektFqNames, trace)

  override val ctx: Context get() = this

  override fun isDenotable(type: TypeRef): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toClassifierRef() }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.collection.toClassifierRef() }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.nullableNothingType.toTypeRef() }
  val anyType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.anyType.toTypeRef() }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val sourceKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames().sourceKey)
    )?.toClassifierRef()
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames().typeKey)
    )?.toClassifierRef()
  }
}
