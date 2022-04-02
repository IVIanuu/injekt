/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag

interface Elements<N> {
  operator fun <T> invoke(@Inject key: TypeKey<T>): T
}

@Provide class ElementsImpl<N>(
  private val key: TypeKey<Elements<N>>,
  elements: List<ProvidedElement<N, *>>
) : Elements<N> {
  @OptIn(ExperimentalStdlibApi::class)
  private val elements = buildMap<String, Any> {
    for ((key, element) in elements)
      this[key.value] = element
  }

  override fun <T> invoke(@Inject key: TypeKey<T>): T =
    elements[key.value] as T
      ?: error("No element found for ${key.value} in ${this.key.value}")
}

@Tag annotation class Element<N> {
  companion object {
    @Provide class Module<@Spread T : @Element<N> S, S : Any, N> {
      @Provide fun provided(key: TypeKey<S>, element: T) = ProvidedElement<N, S>(key, element)

      @Provide inline fun accessor(value: T): S = value
    }
  }
}

data class ProvidedElement<N, T : Any>(val key: TypeKey<T>, val element: T) {
  companion object {
    @Provide fun <N> defaultElements(): Collection<ProvidedElement<N, *>> = emptyList()
  }
}

@Tag annotation class Eager<N> {
  companion object {
    @Provide class Module<@Spread T : @Eager<N> S, S : Any, N> {
      @Provide fun scoped(value: T): @Scoped<N> S = value

      @Provide fun element(value: S): @Element<N> @Initializer S = value

      @Tag private annotation class Initializer
    }
  }
}
