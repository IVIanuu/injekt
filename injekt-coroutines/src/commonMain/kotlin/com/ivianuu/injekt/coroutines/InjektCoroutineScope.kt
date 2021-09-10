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
import com.ivianuu.injekt.scope.DefaultSourceKey
import com.ivianuu.injekt.scope.Disposable
import com.ivianuu.injekt.scope.Scope
import com.ivianuu.injekt.scope.ScopeElement
import com.ivianuu.injekt.scope.ScopeObserver
import com.ivianuu.injekt.scope.scoped
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun launch(
  @Inject scope: Scope,
  block: suspend CoroutineScope.() -> Unit
): Job = scopedCoroutineScope().launch(block = block)

fun launchedEffect(
  vararg args: Any?,
  @Inject key: @DefaultSourceKey Any,
  @Inject scope: Scope,
  block: suspend CoroutineScope.() -> Unit
): Disposable = scoped(key, *args) {
  LaunchedEffectImpl(block = block)
}

private class LaunchedEffectImpl(
  @Inject private val scope: Scope,
  private val block: suspend CoroutineScope.() -> Unit
) : ScopeObserver {
  private var job: Job? = null

  override fun init() {
    job?.cancel()
    job = scopedCoroutineScope().launch(block = block)
  }

  override fun dispose() {
    job?.cancel()
    job = null
  }
}

inline fun scopedCoroutineScope(
  key: Any = typeKeyOf<CoroutineScope>(),
  @Inject scope: Scope,
  context: () -> CoroutineContext = { EmptyCoroutineContext }
): CoroutineScope = scoped(key) { DisposableCoroutineScope(context()) }

/**
 * A [CoroutineScope] which is bound to the lifecycle of the [Scope] S
 *
 * [CoroutineContext] of the scope can be specified with a injectable [InjektCoroutineContext]<S> and
 * defaults to [DefaultDispatcher]
 */
typealias InjektCoroutineScope<S> = CoroutineScope

/**
 * Installs a [InjektCoroutineScope] for scope [S]
 */
@Provide inline fun <S : Scope> injektCoroutineScopeElement(
  scope: S,
  nameKey: TypeKey<S>,
  context: () -> InjektCoroutineContext<S>
): @ScopeElement<S> InjektCoroutineScope<S> = scopedCoroutineScope(
  typeKeyOf<InjektCoroutineScope<S>>()
) { context() }

@PublishedApi internal class DisposableCoroutineScope(
  context: CoroutineContext
) : CoroutineScope, Disposable {
  override val coroutineContext: CoroutineContext = context + SupervisorJob()

  override fun dispose() {
    coroutineContext.cancel()
  }
}

/**
 * [CoroutineContext] of a [InjektCoroutineScope]
 */
typealias InjektCoroutineContext<S> = CoroutineContext

/**
 * The default [InjektCoroutineContext] for scope [S]
 */
@Provide inline fun <S : Scope> injektCoroutineContext(
  dispatcher: DefaultDispatcher
): InjektCoroutineContext<S> = dispatcher
