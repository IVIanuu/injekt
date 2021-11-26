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
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag

interface Elements<N> {
  operator fun <T> invoke(@Inject key: TypeKey<T>): T
}

@Provide class ElementsImpl<N>(elements: List<ProvidedElement<N, *>>) : Elements<N> {
  @OptIn(ExperimentalStdlibApi::class)
  private val elements = buildMap<String, () -> Any> {
    for ((key, factory) in elements)
      this[key.value] = factory
  }

  override fun <T> invoke(@Inject key: TypeKey<T>): T =
    elements[key.value]?.invoke() as T ?: error("No element found for ${key.value}")
}

@Tag annotation class Element<N> {
  companion object {
    @Provide class Module<@com.ivianuu.injekt.Spread T : @Element<N> S, S : Any, N> {
      @Provide fun provided(key: TypeKey<S>, elementFactory: () -> T) =
        ProvidedElement<N, S>(key, elementFactory)

      @Provide inline fun accessor(value: T): S = value
    }
  }
}

data class ProvidedElement<N, T : Any>(val key: TypeKey<T>, val elementFactory: () -> T) {
  companion object {
    @Provide fun <N> defaultElements(): Collection<ProvidedElement<N, *>> = emptyList()
  }
}
