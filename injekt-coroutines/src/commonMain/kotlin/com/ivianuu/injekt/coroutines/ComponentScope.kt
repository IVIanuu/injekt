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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.Disposable
import com.ivianuu.injekt.common.Scoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

typealias ComponentScope<C> = CoroutineScope

@Provide fun <C : @Component Any> componentScope(
  context: ComponentCoroutineContext<C>
): @Scoped<C> ComponentScope<C> = DisposableCoroutineScope<C>(context)

private class DisposableCoroutineScope<C : @Component Any>(
  context: CoroutineContext
) : CoroutineScope, Disposable {
  override val coroutineContext: CoroutineContext = context + SupervisorJob()
  override fun dispose() {
    coroutineContext.cancel()
  }
}

/**
 * [CoroutineContext] of a [ComponentScope]
 */
typealias ComponentCoroutineContext<C> = CoroutineContext

/**
 * The default [ComponentCoroutineContext] for component [C]
 */
@Provide inline fun <C : @Component Any> componentCoroutineContext(
  dispatcher: DefaultDispatcher
): ComponentCoroutineContext<C> = dispatcher
