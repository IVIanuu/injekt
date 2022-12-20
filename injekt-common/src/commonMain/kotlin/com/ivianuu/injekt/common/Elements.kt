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
  private val elements = buildMap {
    for (element in elements)
      this[element.key.value] = element
  }

  init {
    elements.forEach { it.init() }
  }

  override fun <T> invoke(@Inject key: TypeKey<T>): T =
    elements[key.value]?.get() as T
      ?: error("No element found for ${key.value} in ${this.key.value}")
}

@Tag annotation class Element<N> {
  companion object {
    @Provide inline fun <@Spread T : @Element<N> S, S : Any, N> element(
      key: TypeKey<S>,
      crossinline factory: () -> T
    ): ProvidedElement<N, S> = object : ProvidedElement<N, S> {
      override val key: TypeKey<S>
        get() = key

      override fun get(): T = factory()
    }

    @Provide inline fun <@Spread T : @Element<N> S, S : Any, N> accessor(element: T): S = element
  }
}

interface ProvidedElement<N, T : Any> {
  val key: TypeKey<T>

  fun init() {
  }

  fun get(): T

  companion object {
    @Provide fun <N> defaultElements() = emptyList<ProvidedElement<N, *>>()
  }
}

@Tag annotation class Eager<N> {
  companion object {
    @Provide fun <@Spread T : @Eager<N> S, S : Any, N> scoped(value: T): @Scoped<N> S = value

    @Provide inline fun <@Spread T : @Eager<N> S, S : Any, N> element(
      key: TypeKey<S>,
      crossinline factory: () -> S
    ): ProvidedElement<N, S> = object : ProvidedElement<N, @Initializer S> {
      override val key: TypeKey<S>
        get() = key

      private var _value: Any? = null
      override fun init() {
        _value = factory()
      }

      override fun get(): S = _value as S
    }

    @Tag private annotation class Initializer
  }
}
