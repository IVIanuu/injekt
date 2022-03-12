/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlinx.atomicfu.locks.*

interface Scope<N> : Disposable {
  val isDisposed: Boolean

  operator fun <T : Any> invoke(@Inject key: TypeKey<T>, init: () -> T): T
}

fun <N> Scope(): Scope<N> = ScopeImpl()

private class ScopeImpl<N> : SynchronizedObject(), Scope<N>, Disposable {
  private val values = mutableMapOf<String, Any>()
  override var isDisposed = false

  override fun <T : Any> invoke(@Inject key: TypeKey<T>, init: () -> T): T = synchronized(this) {
    check(!isDisposed) { "Cannot use a disposed scope" }
    values.getOrPut(key.value, init) as T
  }

  override fun dispose() {
    synchronized(this) {
      if (!isDisposed) {
        isDisposed = true
        values.values.toList()
          .also { values.clear() }
      } else null
    }?.forEach {
      (it as? Disposable)?.dispose()
    }
  }
}

@Tag annotation class Scoped<N> {
  companion object {
    @Provide inline fun <@Spread T : @Scoped<N> S, S : Any, N> scoped(
      crossinline init: () -> T,
      scope: Scope<N>,
      key: TypeKey<S>
    ): S = scope(key) { init() }
  }
}
