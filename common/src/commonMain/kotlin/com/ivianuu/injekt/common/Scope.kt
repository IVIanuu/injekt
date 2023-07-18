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

class Scope<N> : SynchronizedObject() {
  @PublishedApi internal val values = hashMapOf<Any, Any?>()

  inline operator fun <T> invoke(key: Any, init: () -> T): T = synchronized(this) {
    val value = values.getOrPut(key) { init() ?: NULL }
    (if (value !== NULL) value else null) as T
  }

  inline operator fun <T> invoke(@Inject key: TypeKey<T>, init: () -> T): T =
    invoke(key.value, init)

  companion object {
    @PublishedApi internal val NULL = Any()
  }
}

@Tag @Target(AnnotationTarget.TYPE) annotation class Scoped<N> {
  @Provide companion object {
    @Provide inline fun <@Spread T : @Scoped<N> S, S : Any, N> scoped(
      scope: Scope<N>,
      key: TypeKey<S>,
      crossinline init: () -> T,
    ): S = scope(key) { init() }
  }
}
