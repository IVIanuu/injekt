/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlinx.atomicfu.locks.*
import kotlin.jvm.*

interface Scope<N : Scope.Name> : Disposable {
  operator fun <T : Any> invoke(@Inject key: TypeKey<T>, init: () -> T): T
  interface Name
}

fun <N : Scope.Name> Scope(): Scope<N> = ScopeImpl()

private class ScopeImpl<N : Scope.Name> : SynchronizedObject(), Scope<N>, Disposable {
  private val values = mutableMapOf<String, Any>()

  override fun <T : Any> invoke(@Inject key: TypeKey<T>, init: () -> T): T =
    synchronized(this) { values.getOrPut(key.value, init) as T }

  override fun dispose() {
    synchronized(this) {
      values.values.toList()
        .also { values.clear() }
    }.forEach {
      (it as? Disposable)?.dispose()
    }
  }
}
