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
 * A [CoroutineScope] which is bound to the lifecycle of the [Scope] S
 *
 * [CoroutineContext] of the scope can be specified with a injectable [InjectCoroutineContext]<S> and
 * defaults to [DefaultDispatcher]
 */
typealias InjectCoroutineScope<S> = CoroutineScope

/**
 * Installs a [InjectCoroutineScope] for scope [S]
 */
@Provide fun <S : Scope> injectCoroutineScopeElement(context: InjectCoroutineContext<S>):
    @Scoped<S> @InstallElement<S> InjectCoroutineScope<S> =
  object : CoroutineScope, ScopeDisposable {
    override val coroutineContext: CoroutineContext = context + SupervisorJob()
    override fun dispose() {
      coroutineContext.cancel()
    }
  }

/**
 * Returns the [CoroutineScope] bound to this scope
 */
val Scope.coroutineScope: CoroutineScope get() = element()

/**
 * Installs a [CoroutineScope] for scope [S]
 */
@Provide inline fun <S : Scope> coroutineScopeElement(scope: InjectCoroutineScope<S>):
    @InstallElement<S> CoroutineScope = scope

/**
 * [CoroutineContext] of a [InjectCoroutineScope]
 */
typealias InjectCoroutineContext<S> = CoroutineContext

/**
 * The default [InjectCoroutineContext] for type [S]
 */
@Provide inline fun <S : Scope> defaultInjectCoroutineContext(dispatcher: DefaultDispatcher):
    InjectCoroutineContext<S> = dispatcher
