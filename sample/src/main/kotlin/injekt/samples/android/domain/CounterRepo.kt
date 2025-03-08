/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.domain

import injekt.*
import injekt.common.*
import injekt.samples.android.app.*
import kotlinx.coroutines.flow.*

interface CounterRepo {
  val counter: Flow<Int>

  suspend fun updateCounter(transform: (Int) -> Int)
}

// provide repo impl, scope it to AppScope
// no need to bind the type or something
// injekt sees that CounterRepoImpl is sub type of CounterRepo
@Provide @Scoped<AppScope> class CounterRepoImpl : CounterRepo {
  private val _counter = MutableStateFlow(0)
  override val counter: Flow<Int> by this::_counter

  override suspend fun updateCounter(transform: (Int) -> Int) = _counter.update(transform)
}
