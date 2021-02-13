/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.samples.android.CounterAction.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

object CounterKey

@KeyUiBinding<CounterKey>
@Given
fun counterKeyUi(
    @Given stateProvider: @Composable () -> @UiState CounterState,
    @Given dispatch: Dispatch<CounterAction>
): @Composable () -> Unit = {
    val state = stateProvider()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Count ${state.count}")
        Button(onClick = { dispatch(Inc) }) {
            Text("Inc")
        }
        Button(onClick = { dispatch(Dec) }) {
            Text("Dec")
        }
    }
}

data class CounterState(val count: Int)

sealed class CounterAction {
    object Inc : CounterAction()
    object Dec : CounterAction()
}

@Given
fun counterState(
    @Given actions: Actions<CounterAction>,
    @Given scope: CoroutineScope,
): StateFlow<CounterState> {
    return actions
        .scan(CounterState(0)) { currentState, action ->
            currentState.copy(
                count = when (action) {
                    Inc -> currentState.count.inc()
                    Dec -> currentState.count.dec()
                }
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, CounterState(0))
}
