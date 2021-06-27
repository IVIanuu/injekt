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
  @InternalScopeApi fun <T : Any> getScopedValueOrNull(key: Any): T?

  /**
   * Store's [value] for [key]
   *
   * If [value] is a [Disposable] [Disposable.dispose] will be invoked once this scope gets disposed
   */
  @InternalScopeApi fun <T : Any> setScopedValue(key: Any, value: T)

  /**
   * Removes the scoped value for [key]
   */
  @InternalScopeApi fun removeScopedValue(key: Any)

  /**
   * Returns the element for [key] or throws if it doesn't exist
   */
  fun <T> element(@Inject key: TypeKey<T>): T

  companion object {
    @Tag private annotation class Parent

    @OptIn(InternalScopeApi::class)
    @Provide
    fun <S : Scope> default(
      key: TypeKey<S>,
      parent: @Parent Scope? = null,
      elementsFactory: (
        @Provide S,
        @Provide @Parent Scope?
      ) -> Set<ScopeElementPair<S>> = { _, _ -> emptySet() },
      observersFactory: (
        @Provide S,
        @Provide @Parent Scope?
      ) -> Set<ScopeObserver<S>> = { _, _ -> emptySet() }
    ): S {
      val scope = ElementScopeImpl(key, parent as? ElementScopeImpl)
      @Suppress("UNCHECKED_CAST")
      scope as S

      if (parent != null)
        ParentScopeDisposable(scope).bind(parent)

      val elements = elementsFactory(scope, scope)

      scope.elements = if (elements.isEmpty()) emptyMap()
      else HashMap<String, () -> Any>(elements.size).apply {
        for (elementPair in elements)
          this[elementPair.key.value] = elementPair.factory
      }

      val observers = observersFactory(scope, scope)

      for (observer in observers)
        invokeOnDispose(scope) { observer.onDispose() }

      for (observer in observers)
        observer.onInit()

      return scope
    }
  }
}

/**
 * Lifecycle observer for [Scope] of [S]
 */
interface ScopeObserver<S : Scope> {
  /**
   * Will be called when the scope gets initialized
   */
  fun onInit() {
  }

  /**
   * Will be called when the scope gets disposed
   */
  fun onDispose() {
  }
}

class ScopeElementPair<S : Scope>(val key: TypeKey<*>, val factory: () -> Any)

/**
 * Registers the declaration in the scope [S]
 *
 * Example:
 * ```
 * @Provide
 * @ScopeElement<AppScope>
 * class MyAppDeps(val api: Api, val database: Database)
 *
 * fun runApp(@Inject appScope: AppScope) {
 *   val deps = appScope.element<MyAppDeps>()
 * }
 * ```
 */
@Tag annotation class ScopeElement<S : Scope> {
  companion object {
    @Provide class Module<@Spread T : @ScopeElement<S> U, U : Any, S : Scope> {
      @Provide inline fun elementPair(
        noinline factory: () -> T,
        key: TypeKey<U>
      ): ScopeElementPair<S> = ScopeElementPair(key, factory)

      @Provide inline fun elementAccessor(scope: S, key: TypeKey<U>): U = scope.element(key)
    }
  }
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
  scope.getScopedValueOrNull<T>(key)?.let { return it }
  scope.withLock {
    scope.getScopedValueOrNull<T>(key)?.let { return it }
    val value = computation()
    scope.setScopedValue(key, value)
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
    scope.setScopedValue(disposable, disposable)
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
fun DisposableScope(): DisposableScope = DisposableScopeImpl()

@InternalScopeApi expect inline fun <T> synchronized(lock: Any, block: () -> T): T

@InternalScopeApi
private class ElementScopeImpl(
  private val key: TypeKey<*>,
  private val parent: ElementScopeImpl?
) : DisposableScopeImpl() {
  lateinit var elements: Map<String, () -> Any>

  override fun <T> element(@Inject key: TypeKey<T>): T =
    elementOrNull() ?: error("No element for ${key.value} in ${this.key.value}")

  @Suppress("UNCHECKED_CAST")
  private fun <T> elementOrNull(@Inject key: TypeKey<T>): T? =
    elements[key.value]?.invoke() as? T ?: parent?.elementOrNull(key)
}

@InternalScopeApi
private open class DisposableScopeImpl : DisposableScope {
  override fun <T> element(@Inject key: TypeKey<T>): T =
    throw UnsupportedOperationException("Not support")

  private var _isDisposed = false
  override val isDisposed: Boolean
    get() {
      if (_isDisposed) return true
      return synchronized(this) { _isDisposed }
    }

  private val values = hashMapOf<Any, Any>()

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> getScopedValueOrNull(key: Any): T? = values[key] as? T

  override fun <T : Any> setScopedValue(key: Any, value: T) {
    synchronizedWithDisposedCheck {
      removeAndDispose(key)
      values[key] = value
    } ?: kotlin.run {
      (value as? Disposable)?.dispose()
    }
  }

  override fun removeScopedValue(key: Any) {
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

private class ParentScopeDisposable(scope: DisposableScope) : Disposable {
  private var scope: DisposableScope? = scope

  init {
    // do not leak a reference to the child scope
    invokeOnDispose(scope) { this.scope = null }
  }

  override fun dispose() {
    scope?.dispose()
    scope = null
  }
}
