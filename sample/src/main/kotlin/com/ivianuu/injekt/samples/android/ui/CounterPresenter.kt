package com.ivianuu.injekt.samples.android.ui

import androidx.compose.runtime.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.samples.android.data.*
import kotlinx.coroutines.*

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
