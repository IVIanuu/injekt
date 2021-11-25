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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

interface Scope<N : ComponentName> {
  fun <T : Any> scope(@Inject key: TypeKey<T>, init: () -> T): T
}

@Provide @ComponentElement<N> class ScopeImpl<N : ComponentName> : Scope<N>, Disposable {
  private val values = mutableMapOf<String, Any>()
  private val lock = reentrantLock()

  private val isDisposed = atomic(false)

  override fun <T : Any> scope(@Inject key: TypeKey<T>, init: () -> T): T =
    lock.withLock { values.getOrPut(key.value, init) as T }

  override fun dispose() {
    if (isDisposed.compareAndSet(false, true)) {
      for (value in values.values)
        (value as? Disposable)?.dispose()
    }
  }
}

@Tag annotation class Scoped<N : ComponentName> {
  companion object {
    @Provide fun <@Spread T : @Scoped<N> S, S : Any, N : ComponentName> scoped(
      init: () -> T,
      scope: Scope<N>,
      key: TypeKey<S>
    ): S = scope.scope(key, init)
  }
}

@Tag annotation class Eager<N : ComponentName> {
  companion object {
    @Provide class Module<@Spread T : @Eager<N> S, S : Any, N : ComponentName> {
      @Provide fun scoped(value: T): @Scoped<N> S = value

      @Provide fun initializer(value: S): @ComponentElement<N> @Initializer S = value

      @Tag private annotation class Initializer
    }
  }
}
