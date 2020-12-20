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
import com.ivianuu.injekt.GivenFun
import com.ivianuu.injekt.GivenGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

object CounterKey

@GivenGroup val counterKeyUiBinding = keyUiBinding<CounterKey, CounterPage>()

@GivenFun @Composable fun CounterPage(
    @Given state: @UiState CounterState,
    @Given dispatch: Dispatch<CounterAction>,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Count ${state.count}")
        Button(onClick = { dispatch(CounterAction.Inc) }) {
            Text("Inc")
        }
        Button(onClick = { dispatch(CounterAction.Dec) }) {
            Text("Dec")
        }
    }
}

data class CounterState(val count: Int)

sealed class CounterAction {
    object Inc : CounterAction()
    object Dec : CounterAction()
}

@Given fun counterState(
    @Given actions: Actions<CounterAction>,
    @Given scope: CoroutineScope,
): StateFlow<CounterState> {
    return actions
        .scan(CounterState(0)) { currentState, action ->
            currentState.copy(
                count = when (action) {
                    CounterAction.Inc -> currentState.count.inc()
                    CounterAction.Dec -> currentState.count.dec()
                }
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, CounterState(0))
}
