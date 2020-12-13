package com.ivianuu.injekt.samples.android

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.given
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

object CounterKey

@GivenSet fun counterKeyUiBinding() =
    keyUiWithStateSetOf<CounterKey, CounterState> { CounterPage() }

@Composable
private fun CounterPage(
    state: CounterState = given,
    dispatch: Dispatch<CounterAction> = given,
) {
    Column {
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
    actions: Actions<CounterAction> = given,
    scope: CoroutineScope = given,
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
