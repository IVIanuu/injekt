/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlinx.atomicfu.locks.*

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
