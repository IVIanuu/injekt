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

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * A [CoroutineScope] which is bound to the lifecycle of the [GivenScope] S
 *
 * [CoroutineContext] of the scope can be specified with a given [GivenCoroutineContext]<S> and
 * defaults to [DefaultDispatcher]
 */
typealias GivenCoroutineScope<S> = CoroutineScope

/**
 * Installs a [GivenCoroutineScope] for scope [S]
 */
@Given
fun <S : GivenScope> givenCoroutineScopeElement(
    @Given context: GivenCoroutineContext<S>
): @Scoped<S> @InstallElement<S> GivenCoroutineScope<S> = object : CoroutineScope, GivenScopeDisposable {
    override val coroutineContext: CoroutineContext = context + SupervisorJob()
    override fun dispose() {
        coroutineContext.cancel()
    }
}

/**
 * Returns the [CoroutineScope] bound to this scope
 */
val GivenScope.coroutineScope: CoroutineScope get() = element()

/**
 * Installs a [CoroutineScope] for scope [S]
 */
@Given
inline fun <S : GivenScope> coroutineScopeElement(
    @Given scope: GivenCoroutineScope<S>
): @InstallElement<S> CoroutineScope = scope

/**
 * [CoroutineContext] of a [GivenCoroutineScope]
 */
typealias GivenCoroutineContext<S> = CoroutineContext

/**
 * The default [GivenCoroutineContext] for type [S]
 */
@Given
inline fun <S : GivenScope> defaultGivenCoroutineContext(
    @Given dispatcher: DefaultDispatcher
): GivenCoroutineContext<S> = dispatcher
