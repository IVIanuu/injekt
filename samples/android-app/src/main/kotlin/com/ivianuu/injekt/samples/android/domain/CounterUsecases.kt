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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.data.CounterDb
import kotlinx.coroutines.flow.Flow

typealias CounterFlow = Flow<Int>

@Provide fun counterFlow(db: CounterDb): CounterFlow = db.counterState

typealias IncCounterUseCase = suspend () -> Unit

@Provide fun incCounterUseCase(db: CounterDb): IncCounterUseCase = {
  db.updateCounter { this + 1 }
}

typealias DecCounterUseCase = suspend () -> Unit

@Provide fun decCounterUseCase(db: CounterDb): DecCounterUseCase = {
  db.updateCounter { this - 1 }
}
