package injekt.samples.android.util

import injekt.*
import injekt.common.*
import kotlinx.coroutines.*

// provide a CoroutineScope which is scoped to the lifecycle of Scope instances
// implement ScopeDisposable to properly cancel it once scopes gets disposed
class ScopedCoroutineScope<N> @Provide @Scoped<N> constructor(
) : CoroutineScope by CoroutineScope(Job()), ScopeDisposable {
  override fun dispose() {
    cancel()
  }
}
