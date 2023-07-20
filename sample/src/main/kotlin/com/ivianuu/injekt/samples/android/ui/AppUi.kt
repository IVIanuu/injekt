/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.domain.Counter
import com.ivianuu.injekt.samples.android.domain.DecCounter
import com.ivianuu.injekt.samples.android.domain.IncCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun interface AppUi {
  @Composable operator fun invoke()
}

@Provide fun appUi(modelProvider: CounterModelProvider) = AppUi {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Injekt sample") },
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) {
    val model = modelProvider()
    Column(
      modifier = Modifier
        .padding(it)
        .fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("Count ${model.state}", style = MaterialTheme.typography.subtitle1)
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

fun interface CounterModelProvider {
  @Composable operator fun invoke(): CounterModel
}

@Provide fun counterModelProvider(
  counter: Flow<Counter>,
  incCounter: IncCounter,
  decCounter: DecCounter
) = CounterModelProvider {
  val scope = rememberCoroutineScope()
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
