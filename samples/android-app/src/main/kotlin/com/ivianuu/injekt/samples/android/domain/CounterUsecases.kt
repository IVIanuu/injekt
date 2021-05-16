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

package com.ivianuu.injekt.samples.android.domain

import com.ivianuu.injekt.*
import com.ivianuu.injekt.samples.android.data.*
import kotlinx.coroutines.flow.*

typealias CounterFlow = Flow<Int>

@Given fun counterFlow(@Given storage: CounterStorage): CounterFlow = storage.counterState

typealias IncCounterUseCase = suspend () -> Unit

@Given fun incCounterUseCase(@Given storage: CounterStorage): IncCounterUseCase = {
  storage.updateCounter { this + 1 }
}

typealias DecCounterUseCase = suspend () -> Unit

@Given fun decCounterUseCase(@Given storage: CounterStorage): DecCounterUseCase = {
  storage.updateCounter { this - 1 }
}
