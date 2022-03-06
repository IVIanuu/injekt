/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import kotlinx.atomicfu.locks.*

interface Scope<N> : Disposable {
  operator fun <T : Any> invoke(key: TypeKey<T>, init: () -> T): T
}

inline operator fun <reified T : Any> Scope<*>.invoke(noinline init: () -> T): T =
  this(typeKeyOf(), init)

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
