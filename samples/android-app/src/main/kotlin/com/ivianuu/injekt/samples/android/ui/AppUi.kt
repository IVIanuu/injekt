/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.*
import com.ivianuu.injekt.container.*
import com.ivianuu.injekt.coroutines.*
import com.ivianuu.injekt.samples.android.domain.*
import com.ivianuu.injekt.scope.*
import kotlinx.coroutines.*

typealias AppUi = @Composable () -> Unit

@Provide fun appUi(viewModel: CounterViewModel): AppUi = {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Injekt sample") },
        backgroundColor = MaterialTheme.colors.primary
      )
    }
  ) {
    val state by viewModel.state.collectAsState(0)
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("Count $state", style = MaterialTheme.typography.subtitle1)
      Spacer(Modifier.height(8.dp))
      Button(onClick = { viewModel.inc() }) {
        Text("Inc")
      }
      Spacer(Modifier.height(8.dp))
      Button(onClick = { viewModel.dec() }) {
        Text("Dec")
      }
    }
  }
}

@Provide @Scoped<NamedScope<ActivityRetainedScope>>
class CounterViewModel(
  private val incCounter: IncCounterUseCase,
  private val decCounter: DecCounterUseCase,
  val state: CounterFlow,
  private val scope: NamedCoroutineScope<ActivityRetainedScope>
) {
  fun inc() {
    scope.launch { incCounter() }
  }

  fun dec() {
    scope.launch { decCounter() }
  }
}
