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
import kotlin.annotation.AnnotationTarget.*

data class CounterState(
  val state: Int,
  val eventSink: (CounterEvent) -> Unit
)

sealed interface CounterEvent {
  data object IncCounter : CounterEvent
  data object DecCounter : CounterEvent
}

// directly provide the CounterState here instead of having a wrapper class or interface
// to allow injekt to directly inject state into CounterUi
// this makes code feel more naturally and makes it super easy testable
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

// declare a tag for our CounterUi as return type
// this is important because otherwise it would return just Unit
// which would cause problems if we had multiple screens for example
// the counter presenter above doesn't have this problem
// because its return type CounterState can be considered unique enough
@Tag @Target(TYPE) annotation class CounterUi

// declare CounterUi just like a normal @Composable function
// just add @Provide and and unique return type to make it usable with injekt
@Provide @Composable fun CounterUi(
  state: CounterState,
  // declare modifier
  // in case no modifier is provided injekt still compiles because we have a default here
  modifier: Modifier = Modifier
): @CounterUi Unit {
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("Injekt sample") }) }
  ) {
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
