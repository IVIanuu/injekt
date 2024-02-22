/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlinx.atomicfu.locks.*
import kotlin.reflect.*

class Scope<N> : SynchronizedObject() {
  @PublishedApi internal val values = hashMapOf<Any, Any?>()

  inline operator fun <T> invoke(key: Any, init: () -> T): T = synchronized(this) {
    val value = values.getOrPut(key) { init() ?: NULL }
    (if (value !== NULL) value else null) as T
  }

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
    @Provide inline fun <@Spread T : @Scoped<N> S, reified S : Any, N> scoped(
      scope: Scope<N>,
      crossinline init: () -> T,
    ): S = scope(typeOf<S>()) { init() }
  }
}
