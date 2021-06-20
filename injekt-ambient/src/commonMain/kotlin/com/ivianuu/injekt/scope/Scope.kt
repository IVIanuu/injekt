/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import com.ivianuu.injekt.ambient.synchronized
import com.ivianuu.injekt.common.*

interface Scope {
  /**
   * Whether or not this scope is disposed
   */
  val isDisposed: Boolean

  /**
   * Returns the scoped value [T] for [key] or null
   */
  fun <T : Any> get(key: Any): T?

  fun <T : Any> get(key: TypeKey<T>): T? =
    get(key.value)

  fun <T : Any> get(key: SourceKey): T? =
    get(key.value)

  /**
   * Store's [value] for [key]
   *
   * If [value] is a [ScopeDisposable] [ScopeDisposable.dispose] will be invoked once this scope gets disposed
   */
  fun <T : Any> set(key: Any, value: T)

  fun <T : Any> set(key: TypeKey<T>, value: T) =
    set(key.value, value)

  fun <T : Any> set(key: SourceKey, value: T) =
    set(key.value, value)

  /**
   * Removes the scoped value for [key]
   */
  fun remove(key: Any)

  fun remove(key: TypeKey<*>) = remove(key.value)

  fun remove(key: SourceKey) = remove(key.value)
}

/**
 * The current scope
 */
val AmbientScope = ambientOf<Scope> { GlobalScope }

/**
 * Global scope which will never get disposed
 */
object GlobalScope : Scope by DisposableScope()

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [init]
 */
inline fun <T : Any> Scope.cache(key: Any, init: () -> T): T {
  get<T>(key)?.let { return it }
  withLock {
    get<T>(key)?.let { return it }
    val value = init()
    set(key, value)
    return value
  }
}

inline fun <T : Any> Scope.cache(@Inject key: TypeKey<T>, init: () -> T): T =
  cache(key.value, init)

/**
 * Invokes the [action] function once [this] scope gets disposed
 * or invokes it synchronously if [this] is already disposed
 *
 * Returns a [ScopeDisposable] to unregister the [action]
 */
inline fun Scope.invokeOnDispose(crossinline action: () -> Unit): ScopeDisposable =
  ScopeDisposable { action() }.disposeWith(this)

inline fun <R> Scope.withLock(block: () -> R): R = synchronized(this, block)

private val NoOpScopeDisposable = ScopeDisposable { }

/**
 * Allows scoped values to be notified when the hosting [Scope] gets disposed
 */
fun interface ScopeDisposable {
  /**
   * Get's called while the hosting [Scope] gets disposed via [Scope.dispose]
   */
  fun dispose()
}

/**
 * Disposes this disposable once [scope] gets disposed
 * or synchronously if [scope] is already disposed
 *
 * Returns a [ScopeDisposable] to unregister for disposables
 */
fun ScopeDisposable.disposeWith(scope: Scope): ScopeDisposable {
  if (scope.isDisposed) {
    dispose()
    return NoOpScopeDisposable
  }
  scope.withLock {
    if (scope.isDisposed) {
      dispose()
      return NoOpScopeDisposable
    }

    class DisposableKey

    val key = DisposableKey()
    var notifyDisposal = true
    scope.set(key, ScopeDisposable {
      if (notifyDisposal) dispose()
    })
    return ScopeDisposable {
      notifyDisposal = false
      scope.remove(key)
    }
  }
}

/**
 * A mutable version of [Scope] which is also a [ScopeDisposable]
 */
interface DisposableScope : Scope, ScopeDisposable

/**
 * Returns a new [DisposableScope]
 */
fun DisposableScope(): DisposableScope = DisposableScopeImpl()

private class DisposableScopeImpl : DisposableScope {
  private var _isDisposed = false
  override val isDisposed: Boolean
    get() {
      if (_isDisposed) return true
      return synchronized(this) { _isDisposed }
    }

  private var values: MutableMap<Any, Any>? = null
  private fun values(): MutableMap<Any, Any> =
    (values ?: hashMapOf<Any, Any>().also { values = it })

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> get(key: Any): T? = values?.get(key) as? T

  override fun <T : Any> set(key: Any, value: T) {
    synchronizedWithDisposedCheck {
      removeScopedValueImpl(key)
      values()[key] = value
    } ?: kotlin.run {
      (value as? ScopeDisposable)?.dispose()
    }
  }

  override fun remove(key: Any) {
    synchronizedWithDisposedCheck { removeScopedValueImpl(key) }
  }

  override fun dispose() {
    synchronizedWithDisposedCheck {
      _isDisposed = true
      if (values != null && values!!.isNotEmpty()) {
        values!!.keys
          .toList()
          .forEach { removeScopedValueImpl(it) }
      }
    }
  }

  private fun removeScopedValueImpl(key: Any) {
    (values?.remove(key) as? ScopeDisposable)?.dispose()
  }

  private inline fun <R> synchronizedWithDisposedCheck(block: () -> R): R? {
    if (_isDisposed) return null
    synchronized(this) {
      if (_isDisposed) return null
      return block()
    }
  }
}

typealias NamedScope<N> = Scope
