/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.data

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.samples.android.app.AppScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface CounterDb {
  val counter: Flow<Int>

  suspend fun updateCounter(transform: Int.() -> Int)
}

@Provide @Scoped<AppScope> class CounterDbImpl : CounterDb {
  private val _counter = MutableStateFlow(0)
  override val counter: Flow<Int> by this::_counter

  override suspend fun updateCounter(transform: Int.() -> Int) = _counter.update(transform)
}
