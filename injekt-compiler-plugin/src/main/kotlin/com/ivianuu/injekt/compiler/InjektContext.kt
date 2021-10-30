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
import com.ivianuu.injekt_shaded.Inject1
import com.ivianuu.injekt_shaded.Inject2
import com.ivianuu.injekt_shaded.Provide
import com.ivianuu.injekt_shaded.inject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

typealias WithInjektContext = Inject2<InjektContext, BindingTrace?>

@Inject1<InjektContext> inline val context: InjektContext
  get() = inject()

@Inject1<InjektContext> inline val injektFqNames: InjektFqNames
  get() = context.injektFqNames

@Inject1<BindingTrace?> inline val trace: BindingTrace?
  get() = inject()

@Inject1<InjektContext> inline val module: ModuleDescriptor
  get() = context.module

@Suppress("NewApi")
class InjektContext(
  val module: ModuleDescriptor,
  val injektFqNames: InjektFqNames
) : TypeCheckerContext {
  override val injektContext: InjektContext
    get() = this

  override fun isDenotable(type: TypeRef): Boolean = true

  @Provide private val trace = DelegatingBindingTrace(BindingContext.EMPTY, "injekt-context")

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toClassifierRef() }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.collection.toClassifierRef() }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.nullableNothingType.toTypeRef() }
  val anyType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.anyType.toTypeRef() }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val typeKeyType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames.typeKey)
    )!!.toClassifierRef()
  }
  val componentObserverType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames.componentObserver)
    )!!.toClassifierRef()
  }
  val disposableType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames.disposable)
    )!!.toClassifierRef()
  }
}
