/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class Scope<N> : SynchronizedObject(), Disposable {
  @PublishedApi internal val values = hashMapOf<Any, Any>()
  @PublishedApi internal var _isDisposed = false
  val isDisposed: Boolean
    get() = synchronized(this) { _isDisposed }

  inline operator fun <T : Any> invoke(key: Any, init: () -> T): T = synchronized(this) {
    check(!_isDisposed) { "Cannot use a disposed scope" }
    values.getOrPut(key, init) as T
  }

  inline operator fun <T : Any> invoke(@Inject key: TypeKey<T>, init: () -> T): T =
    invoke(key.value, init)

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

@Tag annotation class Scoped<N> {
  companion object {
    @Provide inline fun <@Spread T : @Scoped<N> S, S : Any, N> scoped(
      crossinline init: () -> T,
      scope: Scope<N>,
      key: TypeKey<S>
    ): S = scope(key) { init() }
  }
}
