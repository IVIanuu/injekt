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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*

@Given @Scoped<AppGivenScope>
class CounterStorage {
  private val _counterState = MutableStateFlow(0)
  val counterState: Flow<Int> by this::_counterState
  private val counterMutex = Mutex()

  suspend fun updateCounter(transform: Int.() -> Int) = counterMutex.withLock {
    _counterState.value = transform(_counterState.value)
  }
}
