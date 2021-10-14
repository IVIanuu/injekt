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

package com.ivianuu.injekt.common

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Component

interface ComponentObserver<C : @Component Any> {
  fun onInit(component: C) {
  }

  fun onDispose(component: C) {
  }
}

interface ComponentSlot<C : @Component Any, T> {
  fun get(): T?
  fun set(value: T)
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Scoped<C : @Component Any>

@Target(AnnotationTarget.CLASS)
annotation class EntryPoint<C : @Component Any>
