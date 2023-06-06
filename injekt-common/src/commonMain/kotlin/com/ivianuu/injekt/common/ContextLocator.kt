/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag

interface ContextLocator<N> {
  operator fun <T> invoke(@Inject key: TypeKey<T>): T
}

@Provide class ContextLocatorImpl<N>(
  private val key: TypeKey<ContextLocator<N>>,
  contexts: List<ProvidedContext<N, *>>
) : ContextLocator<N> {
  private val contexts = buildMap {
    for (context in contexts)
      this[context.key.value] = context
  }

  init {
    contexts.forEach { it.init() }
  }

  override fun <T> invoke(@Inject key: TypeKey<T>): T =
    contexts[key.value]?.get() as T
      ?: error("No context found for ${key.value} in ${this.key.value}")
}

@Tag annotation class Locatable<N> {
  companion object {
    @Provide inline fun <@Spread T : @Locatable<N> S, S : Any, N> context(
      key: TypeKey<S>,
      crossinline factory: () -> T
    ): ProvidedContext<N, S> = object : ProvidedContext<N, S> {
      override val key: TypeKey<S>
        get() = key

      override fun get(): T = factory()
    }

    @Provide inline fun <@Spread T : @Locatable<N> S, S : Any, N> accessor(context: T): S = context
  }
}

interface ProvidedContext<N, T : Any> {
  val key: TypeKey<T>

  fun init() {
  }

  fun get(): T

  companion object {
    @Provide fun <N> default() = emptyList<ProvidedContext<N, *>>()
  }
}

@Tag annotation class Eager<N> {
  companion object {
    @Provide fun <@Spread T : @Eager<N> S, S : Any, N> scoped(value: T): @Scoped<N> S = value

    @Provide inline fun <@Spread T : @Eager<N> S, S : Any, N> context(
      key: TypeKey<S>,
      crossinline factory: () -> S
    ): ProvidedContext<N, S> = object : ProvidedContext<N, @Initializer S> {
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
