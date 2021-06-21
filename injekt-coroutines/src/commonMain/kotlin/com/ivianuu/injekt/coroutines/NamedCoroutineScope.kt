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
import com.ivianuu.injekt.ambient.*
import kotlinx.coroutines.*
import kotlinx.coroutines.GlobalScope
import kotlin.coroutines.*

val AmbientCoroutineScope = ambientOf<CoroutineScope> { GlobalScope }

/**
 * A [CoroutineScope] which is bound to the lifecycle of the [Scope] S
 *
 * [CoroutineContext] of the scope can be specified with a injectable [NamedCoroutineContext]<S> and
 * defaults to [DefaultDispatcher]
 */
typealias NamedCoroutineScope<N> = CoroutineScope

/**
 * Installs a [NamedCoroutineScope] for [NamedScope] of [N]
 */
@Provide fun <N> ambientCoroutineScopeValue(
  context: NamedCoroutineContext<N>,
  scope: NamedScope<N>
): NamedProvidedValue<N, NamedCoroutineScope<N>> = AmbientCoroutineScope provides
    scope.cache { DisposableCoroutineScope(context) }

private class DisposableCoroutineScope(
  context: CoroutineContext
) : CoroutineScope, ScopeDisposable {
  override val coroutineContext: CoroutineContext = context + SupervisorJob()
  override fun dispose() {
    coroutineContext.cancel()
  }
}

/**
 * [CoroutineContext] of a [NamedCoroutineScope]
 */
typealias NamedCoroutineContext<N> = CoroutineContext

/**
 * The default [NamedCoroutineContext] for type [N]
 */
@Provide
inline fun <N> namedCoroutineContext(dispatcher: DefaultDispatcher): NamedCoroutineContext<N> =
  dispatcher
