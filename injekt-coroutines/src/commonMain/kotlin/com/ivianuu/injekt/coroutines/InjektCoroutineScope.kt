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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf
import com.ivianuu.injekt.scope.Disposable
import com.ivianuu.injekt.scope.Scope
import com.ivianuu.injekt.scope.ScopeElement
import com.ivianuu.injekt.scope.scoped
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

inline fun scopedCoroutineScope(
  key: Any = typeKeyOf<CoroutineScope>(),
  @Inject scope: Scope,
  context: () -> CoroutineContext = { EmptyCoroutineContext }
): CoroutineScope = scoped(key) { DisposableCoroutineScope(context()) }

@PublishedApi internal class DisposableCoroutineScope(
  context: CoroutineContext
) : CoroutineScope, Disposable {
  override val coroutineContext: CoroutineContext = context + SupervisorJob()

  override fun dispose() {
    coroutineContext.cancel()
  }
}

/**
 * A [CoroutineScope] which is bound to the lifecycle of the [Scope] S
 *
 * [CoroutineContext] of the scope can be specified with a injectable [NamedCoroutineContext]<S> and
 * defaults to [DefaultDispatcher]
 */
typealias NamedCoroutineScope<S> = CoroutineScope

/**
 * Installs a [NamedCoroutineScope] for scope [S]
 */
@Provide inline fun <S : Scope> namedCoroutineScopeElement(
  scope: S,
  nameKey: TypeKey<S>,
  context: () -> NamedCoroutineContext<S>
): @ScopeElement<S> NamedCoroutineScope<S> = scopedCoroutineScope { context() }

/**
 * [CoroutineContext] of a [NamedCoroutineScope]
 */
typealias NamedCoroutineContext<S> = CoroutineContext

/**
 * The default [NamedCoroutineContext] for scope [S]
 */
@Provide inline fun <S : Scope> injektCoroutineContext(
  dispatcher: DefaultDispatcher
): NamedCoroutineContext<S> = dispatcher
