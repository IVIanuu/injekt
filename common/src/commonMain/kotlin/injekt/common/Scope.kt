/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.common

import injekt.*
import kotlinx.atomicfu.locks.*
import kotlin.annotation.AnnotationTarget.*

@Tag @Target(CLASS, CONSTRUCTOR, TYPE) annotation class Scoped<N> {
  @Provide companion object {
    @Provide inline fun <@AddOn T : @Scoped<N> S, S : Any, N> scoped(
      scope: Scope<N>,
      key: TypeKey<S>,
      init: () -> T,
    ): S = scope.get(key) { init() }
  }
}

fun <N> Scope(): Scope<N> = ScopeImpl()

interface Scope<N> : ScopeDisposable {
  @InternalScopeApi fun lock()

  @InternalScopeApi fun get(key: Any): Any?

  @InternalScopeApi fun put(key: Any, value: Any)

  @InternalScopeApi fun unlock()
}

@OptIn(InternalScopeApi::class)
inline fun <T> Scope<*>.get(key: Any, init: () -> T): T {
  get(key)?.let {
    @Suppress("UNCHECKED_CAST")
    return (if (it !== NULL) it else null) as T
  }

  return try {
    lock()
    get(key)?.let {
      @Suppress("UNCHECKED_CAST")
      return (if (it !== NULL) it else null) as T
    }

    val value = init() ?: NULL
    put(key, value)

    @Suppress("UNCHECKED_CAST")
    (if (value !== NULL) value else null) as T
  } finally {
    unlock()
  }
}

@RequiresOptIn annotation class InternalScopeApi

@PublishedApi internal val NULL = Any()

@OptIn(InternalScopeApi::class) private class ScopeImpl<N> : Scope<N> {
  private val values = hashMapOf<Any, Any>()
  private val lock = ReentrantLock()

  override fun get(key: Any): Any? = values[key]

  override fun put(key: Any, value: Any) {
    values[key] = value
  }

  override fun lock() {
    lock.lock()
  }

  override fun unlock() {
    lock.unlock()
  }

  override fun dispose() {
    lock.withLock {
      values.values.toList()
        .also { values.clear() }
    }.forEach { (it as? ScopeDisposable)?.dispose() }
  }
}

fun interface ScopeDisposable {
  fun dispose()
}
