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

package com.ivianuu.injekt.compose

import androidx.compose.runtime.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*

val LocalScope = staticCompositionLocalOf<Scope> { error("No scope provided") }

/**
 * Returns the element [T] of the [LocalScope]
 */
@Composable fun <T : Any> rememberElement(@Inject key: TypeKey<T>): T {
  val scope = LocalScope.current
  return remember(scope) { scope.element() }
}

/**
 * Remember the value produced by [init]. It behaves similarly to [remember],
 * but the stored value will be stored in the [LocalScope] with the [key]
 * or the [currentCompositeKeyHash] if no key was provided
 */
@Composable fun <T : Any> rememberScopedValue(
  vararg inputs: Any?,
  key: Any? = null,
  init: () -> T
): T {
  val finalKey = key ?: currentCompositeKeyHash

  val scope = LocalScope.current
  val value = remember(*inputs) { scope.getScopedValueOrNull(finalKey) ?: init() }

  DisposableEffect(scope, finalKey, value) {
    scope.setScopedValue(finalKey, value)
    onDispose { }
  }
  return value
}
