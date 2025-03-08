/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.common

import injekt.*
import kotlinx.atomicfu.locks.*

class Scope<N> : SynchronizedObject() {
  @PublishedApi internal val values = hashMapOf<Any, Any>()

  inline fun <T> get(key: Any, init: () -> T): T {
    values[key]?.let {
      @Suppress("UNCHECKED_CAST")
      return (if (it !== NULL) it else null) as T
    }
    return synchronized(this) {
      val value = values.getOrPut(key) { init() ?: NULL }
      @Suppress("UNCHECKED_CAST")
      (if (value !== NULL) value else null) as T
    }
  }

  fun dispose() {
    synchronized(this) {
      values.values.toList()
        .also { values.clear() }
    }.forEach { (it as? ScopeDisposable)?.dispose() }
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
      init: () -> T,
    ): S = scope.get(key) { init() }
  }
}
