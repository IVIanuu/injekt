/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.data

import injekt.Provide
import injekt.common.Scoped
import injekt.samples.android.app.*
import kotlinx.coroutines.flow.*

interface CounterDb {
  val counter: Flow<Int>

  suspend fun updateCounter(transform: (Int) -> Int)
}

@Provide @Scoped<AppScope> class CounterDbImpl : CounterDb {
  private val _counter = MutableStateFlow(0)
  override val counter: Flow<Int> by this::_counter

  override suspend fun updateCounter(transform: (Int) -> Int) = _counter.update(transform)
}
