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
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

interface Scope<N> : Disposable {
  @InternalScopeApi fun lock()

  @InternalScopeApi fun unlock()

  @InternalScopeApi fun get(key: Any): Any?

  @InternalScopeApi fun set(key: Any, value: Any)
}

fun <N> Scope(): Scope<N> = ScopeImpl()

@OptIn(InternalScopeApi::class)
@Suppress("UNCHECKED_CAST")
inline operator fun <T : Any> Scope<*>.invoke(@Inject key: TypeKey<T>, init: () -> T): T {
  get(key.value)?.let { return it as T }
  return try {
    lock()
    get(key.value)
      ?.let { it as T }
      ?: init()
        .also { set(key.value, it) }
  } finally {
    unlock()
  }
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class InternalScopeApi

@OptIn(InternalScopeApi::class)
private class ScopeImpl<N> : Scope<N>, Disposable {
  private val values = mutableMapOf<Any, Any>()

  private val lock = reentrantLock()

  override fun lock() = lock.lock()

  override fun unlock() = lock.unlock()

  override fun get(key: Any): Any? = values[key]

  override fun set(key: Any, value: Any) {
    values[key] = value
  }

  override fun dispose() {
    lock.withLock {
      values.values.toList()
        .also { values.clear() }
    }.forEach {
      (it as? Disposable)?.dispose()
    }
  }
}

@Tag annotation class Scoped<N> {
  companion object {
    @Provide inline fun <@Spread T : @Scoped<N> S, S : Any, N> scoped(
      init: () -> T,
      scope: Scope<N>,
      key: TypeKey<S>
    ): S = scope(key, init)
  }
}
