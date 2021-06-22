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
import kotlin.coroutines.*

@Provide val CoroutineContext.ambients: Ambients
  get() = this[AmbientsCoroutineContextElement]?.ambients
    ?: error("No ambients provided in the current coroutine context")

fun Ambients.asCoroutineContext(): CoroutineContext =
  AmbientsCoroutineContextElement(this)

suspend fun <R> withAmbientsContext(
  context: CoroutineContext = EmptyCoroutineContext,
  @Inject ambients: Ambients,
  block: suspend CoroutineScope.() -> R
) = withContext(coroutineContext + context + ambients.asCoroutineContext(),  block)

private class AmbientsCoroutineContextElement(
  val ambients: Ambients
) : AbstractCoroutineContextElement(AmbientsCoroutineContextElement) {
  companion object : CoroutineContext.Key<AmbientsCoroutineContextElement>
}
