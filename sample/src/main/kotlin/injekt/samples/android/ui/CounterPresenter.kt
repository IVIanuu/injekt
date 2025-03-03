package injekt.samples.android.ui

import androidx.compose.runtime.*
import injekt.Provide
import injekt.samples.android.data.*
import injekt.samples.android.util.*
import kotlinx.coroutines.*

data class CounterState(
  val state: Int,
  val incCounter: () -> Unit,
  val decCounter: () -> Unit
)

fun interface CounterPresenter {
  @Composable fun state(): CounterState
}

@Provide fun CounterPresenter(db: CounterDb, scope: ScopedCoroutineScope<ActivityScope>) = CounterPresenter {
  CounterState(
    state = db.counter.collectAsState(0).value,
    incCounter = {
      scope.launch {
        db.updateCounter { it.inc() }
      }
    },
    decCounter = {
      scope.launch {
        db.updateCounter { it.dec() }
      }
    }
  )
}
