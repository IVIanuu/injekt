/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.domain

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.data.CounterDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JvmInline value class Counter(val value: Int)

@Provide fun counter(db: CounterDb): Flow<Counter> = db.counter.map { Counter(it) }

fun interface IncCounter : suspend () -> Unit

@Provide fun incCounter(db: CounterDb) = IncCounter {
  db.updateCounter { inc() }
}

fun interface DecCounter : suspend () -> Unit

@Provide fun decCounter(db: CounterDb) = DecCounter {
  db.updateCounter { dec() }
}
