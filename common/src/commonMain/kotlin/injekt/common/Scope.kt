/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.common

import injekt.AddOn
import injekt.Provide
import injekt.Tag
import injekt.inject
import kotlinx.atomicfu.locks.*

class Scope<N> : SynchronizedObject() {
  @PublishedApi internal val values = hashMapOf<Any, Any?>()

  inline operator fun <T> invoke(key: Any, init: () -> T): T = synchronized(this) {
    val value = values.getOrPut(key) { init() ?: NULL }
    @Suppress("UNCHECKED_CAST")
    (if (value !== NULL) value else null) as T
  }

  inline operator fun <T> invoke(key: TypeKey<T> = inject, init: () -> T): T =
    invoke(key.value, init)

  fun dispose() {
    values.values.toList().forEach { (it as? ScopeDisposable)?.dispose() }
    values.clear()
  }

  companion object {
    @PublishedApi internal val NULL = Any()
  }
}

fun interface ScopeDisposable {
  fun dispose()
}

@Tag
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
annotation class Scoped<N> {
  @Provide companion object {
    @Provide inline fun <@AddOn T : @Scoped<N> S, S : Any, N> scoped(
      scope: Scope<N>,
      key: TypeKey<S>,
      crossinline init: () -> T,
    ): S = scope(key) { init() }
  }
}
