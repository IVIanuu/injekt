/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.coroutines.*
import com.ivianuu.injekt.samples.android.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun interface AppUi : @Composable () -> Unit

@Provide fun appUi(models: @Composable () -> CounterModel) = AppUi {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Injekt sample") },
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) {
    val model = models()
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("Count ${model.state.value}", style = MaterialTheme.typography.subtitle1)
      Spacer(Modifier.height(8.dp))
      Button(onClick = model.incCounter) {
        Text("Inc")
      }
      Spacer(Modifier.height(8.dp))
      Button(onClick = model.decCounter) {
        Text("Dec")
      }
    }
  }
}

data class CounterModel(
  val state: Counter,
  val incCounter: () -> Unit,
  val decCounter: () -> Unit
)

@Provide fun counterModel(
  counter: Flow<Counter>,
  incCounter: IncCounter,
  decCounter: DecCounter,
  scope: NamedCoroutineScope<ActivityScope>
): @Composable () -> CounterModel = {
  CounterModel(
    state = counter.collectAsState(Counter(0)).value,
    incCounter = {
      scope.launch {
        incCounter()
      }
    },
    decCounter = {
      scope.launch {
        decCounter()
      }
    }
  )
}
