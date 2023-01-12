/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.domain

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.data.CounterDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JvmInline value class Counter(val counter: Flow<Int>)

context(CounterDb) @Provide fun counter() = Counter(counter.map { it })

fun interface IncCounter {
  suspend fun incCounter()
}

context(CounterDb) @Provide fun incCounter() = IncCounter {
  updateCounter { inc() }
}

fun interface DecCounter {
  suspend fun decCounter()
}

context(CounterDb) @Provide fun decCounter() = DecCounter {
  updateCounter { dec() }
}
