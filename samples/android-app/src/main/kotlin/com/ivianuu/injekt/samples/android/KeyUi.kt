package com.ivianuu.injekt.samples.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenTuple2
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.Storage
import com.ivianuu.injekt.component.memo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.reflect.KClass

typealias KeyUiBinding = Pair<KClass<*>, @Composable () -> Unit>

inline fun <reified K : Any> keyUiBinding(noinline content: @Composable () -> Unit): KeyUiBinding =
    K::class to content

typealias keyUiWithStateBinding<K, S> = (@Composable @Given GivenTuple2<Component<ApplicationScoped>, S>.() -> Unit) -> KeyUiBinding

@Given inline fun <reified K : Any, S> keyUiWithStateBinding(
    @Given c: Component<ApplicationScoped>,
    @Given noinline stateFactory: (CoroutineScope) -> StateFlow<S>,
): keyUiWithStateBinding<K, S> = { content ->
    keyUiBinding<K> {
        val coroutineScope = rememberCoroutineScope()
        val state = remember { stateFactory(coroutineScope) }.collectAsState().value
        content(GivenTuple2(c, state))
    }
}

typealias ActionChannel<A> = Channel<A>

@Given fun <A> ActionChannel(@Given storage: Storage<ApplicationScoped>): ActionChannel<A> =
    storage.memo("action_channel") { Channel() }

typealias Dispatch<A> = (A) -> Unit

@Given val <A> @Given ActionChannel<A>.dispatch: Dispatch<A>
    get() = { action: A -> offer(action) }

typealias Actions<A> = Flow<A>

@Given inline val <A> @Given ActionChannel<A>.actions: Actions<A>
    get() = consumeAsFlow()
