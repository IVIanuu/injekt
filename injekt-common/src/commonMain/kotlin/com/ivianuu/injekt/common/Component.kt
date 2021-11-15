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

import com.ivianuu.injekt.Inject

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Component

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class EntryPoint<C : @Component Any>

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.TYPE
)
annotation class Scoped<C : @Component Any>(val eager: Boolean = false)

fun interface Disposable {
  fun dispose()
}

inline fun <T> T.dispose(@Inject conversion: Conversion<T, Disposable>) {
  `as`<T, Disposable>().dispose()
}

interface ComponentObserver<C : @Component Any> : Disposable {
  fun init() {
  }

  override fun dispose() {
  }
}

@Component interface AppComponent

internal val componentConversion = Conversion<@Component Any, Any> { it }
