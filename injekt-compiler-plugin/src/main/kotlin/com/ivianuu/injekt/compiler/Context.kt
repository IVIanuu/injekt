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

import com.ivianuu.injekt.compiler.resolution.TypeCheckerContext
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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

  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) {
    module.builtIns.nullableNothingType.toTypeRef()
  }
  val anyType by lazy(LazyThreadSafetyMode.NONE) {
    module.builtIns.anyType.toTypeRef()
  }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val typeKeyClassifier by lazy(LazyThreadSafetyMode.NONE) {
    classifierDescriptorForFqName(injektFqNames.typeKey, NoLookupLocation.FROM_BACKEND)!!
      .toClassifierRef()
  }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) {
    module.builtIns.collection.toClassifierRef()
  }
  val componentObserverClassifier by lazy(LazyThreadSafetyMode.NONE) {
    classifierDescriptorForFqName(injektFqNames.componentObserver, NoLookupLocation.FROM_BACKEND)!!
      .toClassifierRef()
  }
  val disposableClassifier by lazy(LazyThreadSafetyMode.NONE) {
    classifierDescriptorForFqName(injektFqNames.disposable, NoLookupLocation.FROM_BACKEND)!!
      .toClassifierRef()
  }
  val incrementalClassifier by lazy(LazyThreadSafetyMode.NONE) {
    classifierDescriptorForFqName(injektFqNames.incremental, NoLookupLocation.FROM_BACKEND)!!
      .toClassifierRef()
  }
  val incrementalFactoryClassifier by lazy(LazyThreadSafetyMode.NONE) {
    classifierDescriptorForFqName(injektFqNames.incrementalFactory, NoLookupLocation.FROM_BACKEND)!!
      .toClassifierRef()
  }
  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toClassifierRef() }
}
