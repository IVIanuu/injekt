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
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.android.ActivityComponent
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.coroutines.ComponentScope
import com.ivianuu.injekt.samples.android.domain.CounterUsecases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Tag annotation class AppUiTag
typealias AppUi = @AppUiTag @Composable () -> Unit

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

@Provide @Scoped<ActivityComponent> class CounterViewModel(
  private val usecases: CounterUsecases,
  private val scope: ComponentScope<ActivityComponent>
) {
  val state: Flow<Int>
    get() = usecases.counter()

  fun inc() {
    scope.launch { usecases.incCounter() }
  }

  fun dec() {
    scope.launch { usecases.decCounter() }
  }
}
