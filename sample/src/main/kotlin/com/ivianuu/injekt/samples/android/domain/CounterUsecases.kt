/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.domain

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.data.CounterDb
import kotlinx.coroutines.flow.map

@JvmInline value class Counter(val value: Int)

@Provide fun counter(db: CounterDb) = db.counter.map { Counter(it) }

fun interface IncCounter {
  suspend operator fun invoke()
}

@Provide fun incCounter(db: CounterDb) = IncCounter { db.updateCounter { it.inc() } }

fun interface DecCounter {
  suspend operator fun invoke()
}

@Provide fun decCounter(db: CounterDb) = DecCounter { db.updateCounter { it.dec() } }
