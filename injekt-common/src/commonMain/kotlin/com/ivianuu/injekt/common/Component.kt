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
import kotlinx.atomicfu.atomic

interface Component<N : ComponentName> : Disposable {
  fun <T> element(@Inject key: TypeKey<T>): T
}

class ComponentImpl<N : ComponentName> private constructor(
  private val elements: Map<String, Lazy<Any>>
) : Component<N> {
  private val isDisposed = atomic(false)

  override fun <T> element(@Inject key: TypeKey<T>): T =
    elements[key.value]?.value as T ?: error("No element found for ${key.value}")

  override fun dispose() {
    if (isDisposed.compareAndSet(false, true)) {
      for (lazyElement in elements.values)
          (lazyElement.value as? Disposable)?.dispose()
    }
  }

  companion object {
    @OptIn(ExperimentalStdlibApi::class)
    @Provide
    fun <N : ComponentName> create(
      nameKey: TypeKey<N>,
      elementsFactory: (Component<N>) -> List<ProvidedElement<N>>
    ): Component<N> {
      val scope = Scope<N>()

      val elements = mutableMapOf<String, Lazy<Any>>()

      val component = ComponentImpl<N>(elements)

      elements[typeKeyOf<Scope<N>>().value] = lazyOf(scope)
      for ((key, lazyElement) in elementsFactory(component))
        elements[key.value] = lazyElement

      for (element in elements)
        element.value.value

      return component
    }
  }
}

interface ComponentName

@Tag annotation class ComponentElement<N : ComponentName> {
  companion object {
    @Provide class Module<@com.ivianuu.injekt.Spread T : @ComponentElement<N> S, S : Any, N : ComponentName> {
      @Provide fun providedElement(
        key: TypeKey<S>,
        lazyElement: Lazy<T>
      ) = ProvidedElement<N>(key, lazyElement)

      @Provide inline fun elementAccessor(value: T): S = value
    }
  }
}

data class ProvidedElement<N : ComponentName>(
  val key: TypeKey<*>,
  val lazyElement: Lazy<Any>
) {
  companion object {
    @Provide
    fun <N : ComponentName> defaultElements(): Collection<ProvidedElement<N>> = emptyList()
  }
}

object AppComponent : ComponentName
