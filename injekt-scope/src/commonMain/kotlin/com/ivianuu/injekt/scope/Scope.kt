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
import com.ivianuu.injekt.common.*

interface Scope {
  /**
   * Whether or not this scope is disposed
   */
  val isDisposed: Boolean

  /**
   * Returns the scoped value [T] for [key] or null
   */
  @InternalScopeApi fun <T : Any> get(key: Any): T?

  /**
   * Store's [value] for [key]
   *
   * If [value] is a [Disposable] [Disposable.dispose] will be invoked once this scope gets disposed
   */
  @InternalScopeApi fun <T : Any> set(key: Any, value: T)

  /**
   * Removes the scoped value for [key]
   */
  @InternalScopeApi fun remove(key: Any)
}

@RequiresOptIn annotation class InternalScopeApi

/**
 * Runs the [block] with a fresh [Scope] which will be disposed after the execution
 */
inline fun <R> withScope(block: (@Inject Scope) -> R): R {
  @Provide val scope = DisposableScope()
  return try {
    block()
  } finally {
    scope.dispose()
  }
}

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [computation]
 */
@OptIn(InternalScopeApi::class)
inline fun <T : Any> scoped(key: Any, @Inject scope: Scope, computation: () -> T): T {
  scope.get<T>(key)?.let { return it }
  scope.withLock {
    scope.get<T>(key)?.let { return it }
    val value = computation()
    scope.set(key, value)
    return value
  }
}

inline fun <T : Any> scoped(@Inject key: TypeKey<T>, @Inject scope: Scope, computation: () -> T): T =
  scoped(key = key.value, computation = computation)

/**
 * Invokes the [action] function once [scope] gets disposed
 * or invokes it synchronously if [scope] is already disposed
 *
 * Returns a [Disposable] to unregister the [action]
 */
inline fun invokeOnDispose(@Inject scope: Scope, crossinline action: () -> Unit) {
  Disposable { action() }.bind()
}

/**
 * Allows scoped values to be notified when the hosting [Scope] gets disposed
 */
fun interface Disposable {
  /**
   * Will be called when the hosting [Scope] gets disposed
   */
  fun dispose()
}

/**
 * Type class to dispose values of [T]
 */
fun interface Disposer<in T> {
  /**
   * Disposes [value]
   */
  fun dispose(value: T)
}

/**
 * Returns a [Disposable] which disposes [this] using [disposer]
 */
fun <T> T.asDisposable(@Inject disposer: Disposer<T>): Disposable =
  Disposable { disposer.dispose(this) }

/**
 * Disposes this value with [scope] using [disposer]
 */
fun <T> T.bind(@Inject scope: Scope, @Inject disposer: Disposer<T>): T =
  apply { asDisposable().bind() }

/**
 * Disposes this disposable once [scope] gets disposed
 * or synchronously if [scope] is already disposed
 *
 * Returns a [Disposable] to unregister for disposables
 */
@OptIn(InternalScopeApi::class)
fun <T : Disposable> T.bind(@Inject scope: Scope): T {
  if (scope.isDisposed) {
    dispose()
    return this
  }
  scope.withLock {
    if (scope.isDisposed) {
      dispose()
      return this
    }

    val disposable = Disposable { dispose() }
    scope.set(disposable, disposable)
  }

  return this
}

@OptIn(InternalScopeApi::class)
inline fun <R> Scope.withLock(block: () -> R): R = synchronized(this, block)

/**
 * A mutable version of [Scope] which is also a [Disposable]
 */
interface DisposableScope : Scope, Disposable

/**
 * Returns a new [DisposableScope]
 */
@OptIn(InternalScopeApi::class)
fun DisposableScope(): DisposableScope = DisposableImpl()

object SingletonScope : Scope by DisposableScope()

@InternalScopeApi
private class DisposableImpl : DisposableScope {
  private var _isDisposed = false
  override val isDisposed: Boolean
    get() {
      if (_isDisposed) return true
      return synchronized(this) { _isDisposed }
    }

  private val values = hashMapOf<Any, Any>()

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> get(key: Any): T? = values[key] as? T

  override fun <T : Any> set(key: Any, value: T) {
    synchronizedWithDisposedCheck {
      removeAndDispose(key)
      values[key] = value
    } ?: kotlin.run {
      (value as? Disposable)?.dispose()
    }
  }

  override fun remove(key: Any) {
    synchronizedWithDisposedCheck { removeAndDispose(key) }
  }

  override fun dispose() {
    synchronizedWithDisposedCheck {
      _isDisposed = true
      if (values.isNotEmpty()) {
        values.keys
          .toList()
          .forEach { removeAndDispose(it) }
      }
    }
  }

  private fun removeAndDispose(key: Any) {
    (values.remove(key) as? Disposable)?.dispose()
  }

  private inline fun <R> synchronizedWithDisposedCheck(block: () -> R): R? {
    if (_isDisposed) return null
    synchronized(this) {
      if (_isDisposed) return null
      return block()
    }
  }
}

@InternalScopeApi expect inline fun <T> synchronized(lock: Any, block: () -> T): T
