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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.ActivityRetainedScope
import com.ivianuu.injekt.coroutines.InjektCoroutineScope
import com.ivianuu.injekt.samples.android.domain.CounterFlow
import com.ivianuu.injekt.samples.android.domain.DecCounterUseCase
import com.ivianuu.injekt.samples.android.domain.IncCounterUseCase
import com.ivianuu.injekt.scope.Scoped
import kotlinx.coroutines.launch

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

@Provide @Scoped<ActivityRetainedScope>
class CounterViewModel(
  private val incCounter: IncCounterUseCase,
  private val decCounter: DecCounterUseCase,
  val state: CounterFlow,
  private val scope: InjektCoroutineScope<ActivityRetainedScope>
) {
  fun inc() {
    scope.launch { incCounter() }
  }

  fun dec() {
    scope.launch { decCounter() }
  }
}
