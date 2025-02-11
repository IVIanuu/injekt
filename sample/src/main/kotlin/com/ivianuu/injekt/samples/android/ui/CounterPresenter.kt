package com.ivianuu.injekt.samples.android.ui

import androidx.compose.runtime.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.samples.android.app.AppScope
import com.ivianuu.injekt.samples.android.data.*
import com.ivianuu.injekt.samples.android.util.*
import kotlinx.coroutines.*

data class CounterState(val state: Int, val incCounter: () -> Unit, val decCounter: () -> Unit)

@Composable @Contextual fun CounterPresenter(): CounterState {
  val db = resolve<CounterDb>()
  val scope = resolve<ScopedCoroutineScope<AppScope>>()
  return CounterState(
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
