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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag

interface Scope<N : ComponentName> {
  fun <T> scope(key: TypeKey<T>, init: () -> T): T
}

@Provide @ComponentElement<N> class ScopeImpl<N : ComponentName> : Scope<N> {
  private val map = mutableMapOf<String, Any>()

  override fun <T> scope(key: TypeKey<T>, init: () -> T): T =
    map.getOrPut(key.value) { init() ?: Null }.takeIf { it !is Null } as T

  private object Null
}

@Tag annotation class Scoped<N : ComponentName> {
  companion object {
    @Provide fun <@Spread T : @Scoped<N> S, S, N : ComponentName> scoped(
      init: () -> T,
      scope: Scope<N>,
      key: TypeKey<S>
    ): S = scope.scope(key, init)
  }
}
