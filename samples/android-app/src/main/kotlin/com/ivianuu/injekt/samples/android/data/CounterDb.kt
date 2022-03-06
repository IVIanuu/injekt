/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.data

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.samples.android.app.*
import kotlinx.coroutines.flow.*

interface CounterDb {
  val counter: Flow<Int>

  suspend fun updateCounter(transform: Int.() -> Int)
}

class CounterDbImpl : CounterDb {
  private val _counter = MutableStateFlow(0)
  override val counter: Flow<Int> by this::_counter

  override suspend fun updateCounter(transform: Int.() -> Int) = _counter.update(transform)

  companion object {
    @Provide fun counterDb(scope: Scope<AppScope>) = scope(typeKeyOf()) {
      CounterDbImpl()
    }
  }
}
