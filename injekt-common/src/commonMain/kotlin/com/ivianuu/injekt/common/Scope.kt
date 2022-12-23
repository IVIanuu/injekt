/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.inject
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

  context(TypeKey<T>) inline fun <T> scoped(init: () -> T): T =
    scoped(inject<TypeKey<T>>().value, init)

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
    context(Scope<N>) @Provide inline fun <@Spread T : @Scoped<N> S, S : Any, N> scoped(
      crossinline init: () -> T,
      key: TypeKey<S>
    ): S = scoped(key) { init() }
  }
}
