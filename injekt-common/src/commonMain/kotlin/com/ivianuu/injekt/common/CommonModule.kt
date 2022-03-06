/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlin.reflect.*

@Provide object CommonModule {
  /**
   * Provides a [Map] of [K] [V] for each [List] of [Pair] of [K] [V]
   */
  @Provide inline fun <K, V> mapOfPairs(pairs: List<Pair<K, V>>): Map<K, V> = pairs.toMap()

  /**
   * Provides a [KClass] of [T]
   */
  @Provide inline fun <reified T : Any> kClass(): KClass<T> = T::class

  /**
   * Provides a [Lazy] of [T]
   */
  @Provide inline fun <T> lazy(noinline init: () -> T): Lazy<T> = kotlin.lazy(init)
}
