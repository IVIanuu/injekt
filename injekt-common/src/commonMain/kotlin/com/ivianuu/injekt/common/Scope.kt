/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlinx.atomicfu.locks.*
import kotlin.reflect.*

interface Scope<N> : Disposable {
  operator fun <T : Any> invoke(key: TypeKey<T>, init: () -> T): T
}

fun <N> Scope(): Scope<N> = ScopeImpl()

private class ScopeImpl<N> : SynchronizedObject(), Scope<N>, Disposable {
  private val values = mutableMapOf<TypeKey<*>, Any>()

  override fun <T : Any> invoke(key: TypeKey<T>, init: () -> T): T =
    synchronized(this) { values.getOrPut(key, init) as T }

  override fun dispose() {
    synchronized(this) {
      values.values.toList()
        .also { values.clear() }
    }.forEach {
      (it as? Disposable)?.dispose()
    }
  }
}
