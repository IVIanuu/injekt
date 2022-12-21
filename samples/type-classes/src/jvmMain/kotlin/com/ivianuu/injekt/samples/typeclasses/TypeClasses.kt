/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.typeclasses

import com.ivianuu.injekt.Provide

fun interface Ord<in T> {
  fun T.compareTo(x: T): Int

  companion object {
    @Provide val int = Ord<Int> { x -> compareTo(x) }
  }
}

context(Ord<T>) fun <T> List<T>.sortedWithOrd(): List<T> = sortedWith { a, b -> a.compareTo(b) }

fun main() {
  val items = listOf(5, 3, 4, 1, 2).sortedWithOrd()
}
