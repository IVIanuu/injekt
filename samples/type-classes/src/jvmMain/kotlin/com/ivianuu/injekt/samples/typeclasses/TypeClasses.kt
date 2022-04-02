/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.typeclasses

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide

fun interface Ord<in T> {
  fun compare(a: T, b: T): Int

  companion object {
    @Provide val int = Ord<Int> { a, b -> a.compareTo(b) }
  }
}

fun <T> List<T>.sorted(@Inject ord: Ord<T>): List<T> = sortedWith { a, b -> ord.compare(a, b) }

fun main() {
  val items = listOf(5, 3, 4, 1, 2).sorted()
}
