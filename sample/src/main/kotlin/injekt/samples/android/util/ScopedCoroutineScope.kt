package injekt.samples.android.util

import injekt.*
import injekt.common.*
import kotlinx.coroutines.*

@Tag annotation class For<N>

// provide a CoroutineScope which is scoped to the lifecycle of Scope instances
// implement ScopeDisposable to properly cancel it once scopes gets disposed
@Provide fun <N> scopedCoroutineScope(): @Scoped<N> @For<N> CoroutineScope =
  object : CoroutineScope by CoroutineScope(Job()), ScopeDisposable {
    override fun dispose() {
      cancel()
    }
  }
