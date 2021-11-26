/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.samples.android.data

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.samples.android.app.AppComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CounterDb {
  val counter: Flow<Int>

  suspend fun updateCounter(transform: Int.() -> Int)
}

@Provide @Scoped<AppComponent> class CounterDbImpl : CounterDb {
  private val _counter = MutableStateFlow(0)
  override val counter: Flow<Int> by this::_counter
  private val lock = Mutex()

  override suspend fun updateCounter(transform: Int.() -> Int) = lock.withLock {
    _counter.value = transform(_counter.value)
  }
}
