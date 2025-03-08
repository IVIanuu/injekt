/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import injekt.*
import injekt.samples.android.domain.*
import injekt.samples.android.util.*
import kotlinx.coroutines.*

data class CounterState(
  val state: Int,
  val eventSink: (CounterEvent) -> Unit
)

sealed interface CounterEvent {
  data object IncCounter : CounterEvent
  data object DecCounter : CounterEvent
}

@Provide @Composable fun CounterPresenter(
  repo: CounterRepo,
  scope: ScopedCoroutineScope<ActivityScope>
) = CounterState(repo.counter.collectAsState(0).value) { event ->
  scope.launch {
    when (event) {
      CounterEvent.IncCounter -> repo.updateCounter { it + 1 }
      CounterEvent.DecCounter -> repo.updateCounter { it - 1 }
    }
  }
}

@Tag @Target(AnnotationTarget.TYPE) annotation class AppUi

@Provide @Composable fun AppUi(state: CounterState): @AppUi Unit {
  Scaffold(topBar = { TopAppBar(title = { Text("Injekt sample") }) }) {
    Column(
      modifier = Modifier
        .padding(it)
        .fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("Count ${state.state}", style = MaterialTheme.typography.subtitle1)

      Spacer(Modifier.height(8.dp))

      Button(onClick = { state.eventSink(CounterEvent.IncCounter) }) {
        Text("Inc")
      }

      Spacer(Modifier.height(8.dp))

      Button(onClick = { state.eventSink(CounterEvent.DecCounter) }) {
        Text("Dec")
      }
    }
  }
}
