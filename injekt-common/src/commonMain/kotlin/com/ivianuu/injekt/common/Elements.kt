/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

interface Elements<N> {
  operator fun <T> invoke(@Inject key: TypeKey<T>): T
}

@Provide class ElementsImpl<N>(
  private val key: TypeKey<Elements<N>>,
  elements: List<Element<N, *>>
) : Elements<N> {
  @OptIn(ExperimentalStdlibApi::class)
  private val elements = buildMap<String, Any> {
    for (element in elements)
      this[element.key.value] = element.value
  }

  override fun <T> invoke(@Inject key: TypeKey<T>): T =
    elements[key.value] as T
      ?: error("No element found for ${key.value} in ${this.key.value}")
}

data class Element<N, T : Any>(
  @Provide val value: T,
  @Inject val key: TypeKey<T>
)
