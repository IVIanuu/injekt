/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.test

import androidx.compose.runtime.*
import kotlin.coroutines.*

fun <R> runComposing(block: @Composable () -> R): R {
  val recomposer = Recomposer(EmptyCoroutineContext)
  var result: Any? = null
  Composition(UnitApplier, recomposer).run {
    setContent {
      result = block()
    }
  }
  return result as R
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertBottomUp(index: Int, instance: Unit) {}
  override fun insertTopDown(index: Int, instance: Unit) {}
  override fun move(from: Int, to: Int, count: Int) {}
  override fun remove(index: Int, count: Int) {}
  override fun onClear() {}
}
