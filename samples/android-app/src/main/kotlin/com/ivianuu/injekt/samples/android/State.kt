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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Unqualified
import com.ivianuu.injekt.common.ForKey
import com.ivianuu.injekt.common.Scoped
import com.ivianuu.injekt.component.AppComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow

typealias ActionChannel<A> = Channel<A>

@Scoped<AppComponent> @Given fun <@ForKey A> ActionChannel(): ActionChannel<A> = Channel()

typealias Dispatch<A> = (A) -> Unit

@Given val <A> @Given ActionChannel<A>.dispatch: Dispatch<A>
    get() = { action: A -> offer(action) }

typealias Actions<A> = Flow<A>

@Given inline val <A> @Given ActionChannel<A>.actions: Actions<A>
    get() = consumeAsFlow()

@Qualifier annotation class UiState

@Given @Composable
fun <T> uiState(@Given stateFactory: (@Given CoroutineScope) -> StateFlow<@Unqualified T>): @UiState T {
    val scope = rememberCoroutineScope()
    return remember { stateFactory(scope) }.collectAsState().value
}
