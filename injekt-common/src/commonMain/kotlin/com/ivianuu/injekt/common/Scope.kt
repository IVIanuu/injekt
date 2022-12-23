/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class Scope<N> : SynchronizedObject(), Disposable {
  @PublishedApi internal val values = hashMapOf<Any, Any?>()
  @PublishedApi internal var _isDisposed = false
  val isDisposed: Boolean
    get() = synchronized(this) { _isDisposed }

  inline fun <T> scoped(key: Any, init: () -> T): T = synchronized(this) {
    check(!_isDisposed) { "Cannot use a disposed scope" }
    val value = values.getOrPut(key) { init() ?: NULL }
    (if (value !== NULL) value else null) as T
  }

  inline fun <T> scoped(@Context key: TypeKey<T>, init: () -> T): T =
    scoped(key.value, init)

  override fun dispose() {
    synchronized(this) {
      if (!_isDisposed) {
        _isDisposed = true
        values.values.toList()
          .also { values.clear() }
      } else null
    }?.forEach {
      (it as? Disposable)?.dispose()
    }
  }
}

@PublishedApi internal val NULL = Any()

@Tag annotation class Scoped<N> {
  companion object {
    @Provide inline fun <@Spread T : @Scoped<N> S, S : Any, N> scoped(
      crossinline init: () -> T,
      scope: Scope<N>,
      key: TypeKey<S>
    ): S = scope.scoped(key) { init() }
  }
}
