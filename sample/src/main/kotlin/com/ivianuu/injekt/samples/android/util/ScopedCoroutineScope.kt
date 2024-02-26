package com.ivianuu.injekt.samples.android.util

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import kotlinx.coroutines.*

class ScopedCoroutineScope<N> @Provide @Scoped<N> constructor(
) : CoroutineScope by CoroutineScope(Job()), ScopeDisposable {
  override fun dispose() {
    cancel()
  }
}
