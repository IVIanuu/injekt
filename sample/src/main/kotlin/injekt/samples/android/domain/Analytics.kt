/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.domain

import injekt.*
import injekt.samples.android.app.*
import injekt.samples.android.util.*
import kotlinx.coroutines.*

@Provide class Analytics(
  private val scope: @For<AppScope> CoroutineScope
) {
  suspend fun log(message: String) = scope.launch {
    println(message)
  }.join()
}
