package injekt.samples.android.util

import injekt.Provide
import injekt.common.ScopeDisposable
import injekt.common.Scoped
import kotlinx.coroutines.*

class ScopedCoroutineScope<N> @Provide @Scoped<N> constructor(
) : CoroutineScope by CoroutineScope(Job()), ScopeDisposable {
  override fun dispose() {
    cancel()
  }
}
