package com.ivianuu.injekt.samples.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.data.CounterDb
import kotlinx.coroutines.launch

data class CounterState(
  val state: Int,
  val incCounter: () -> Unit,
  val decCounter: () -> Unit
)

fun interface CounterPresenter {
  @Composable operator fun invoke(): CounterState

  @Provide companion object {
    @Provide fun impl(db: CounterDb) = CounterPresenter {
      val scope = rememberCoroutineScope()
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
  }
}
