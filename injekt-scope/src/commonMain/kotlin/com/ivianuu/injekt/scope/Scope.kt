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

@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.Scope.Companion.Scope

/**
 * A hierarchical construct with a lifecycle which hosts a map of keys to elements
 * which can be retrieved via [Scope.element]
 */
interface Scope : ScopeDisposable {
  /**
   * The type key of this scope
   */
  val typeKey: TypeKey<Scope>

  /**
   * The parent scope or null
   */
  val parent: Scope?

  /**
   * Whether or not this scope is disposed
   */
  val isDisposed: Boolean

  /**
   * Returns the element [T] or null
   */
  fun <T> elementOrNull(key: TypeKey<T>): T?

  /**
   * Returns the scoped value [T] for [key] or null
   */
  fun <T : Any> getScopedValueOrNull(key: Any): T?
  fun <T : Any> getScopedValueOrNull(key: TypeKey<T>): T? =
    getScopedValueOrNull(key.value)

  /**
   * Sets the scoped value [T] for [key] to [value]
   */
  fun <T : Any> setScopedValue(key: Any, value: T)
  fun <T : Any> setScopedValue(key: TypeKey<T>, value: T) =
    setScopedValue(key.value, value)

  /**
   * Removes the scoped value for [key]
   */
  fun removeScopedValue(key: Any)
  fun removeScopedValue(key: TypeKey<*>) = removeScopedValue(key.value)

  companion object {
    @Qualifier private annotation class Parent

    @Provide inline fun <S : Scope> Scope(
      parent: @Parent Scope? = null,
      typeKey: TypeKey<S>,
      elements: (@Provide S, @Provide @Parent Scope?) -> Set<ScopeElement<S>> = { _, _ -> emptySet() },
      initializers: (@Provide S, @Provide @Parent Scope?) -> Set<ScopeInitializer<S>> = { _, _ -> emptySet() }
    ): S {
      val scope = ScopeImpl(typeKey, parent)
      scope as S
      val parentDisposable = parent?.invokeOnDispose { scope.dispose() }
      scope.invokeOnDispose { parentDisposable?.dispose() }
      val finalElements = elements(scope, scope)
      scope.elements = if (finalElements.isEmpty()) emptyMap()
      else HashMap<String, () -> Any>(finalElements.size).apply {
        finalElements.forEach { this[it.key.value] = it.factory }
      }
      initializers(scope, scope).forEach { it() }
      return scope
    }
  }
}

/**
 * Allows scoped values to be notified when the hosting [Scope] get's disposed
 */
fun interface ScopeDisposable {
  /**
   * Get's called while the hosting [Scope] get's disposed via [Scope.dispose]
   */
  fun dispose()
}

fun <@ForTypeKey T> Scope.elementOrNull(): T? = elementOrNull(typeKeyOf())

fun <@ForTypeKey T> Scope.element(): T = element(typeKeyOf())

/**
 * Returns the element [T] or throws
 */
fun <T> Scope.element(key: TypeKey<T>): T = elementOrNull(key)
  ?: error("No element found for $key in $this")

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [init]
 */
inline fun <T : Any> Scope.getOrCreateScopedValue(key: Any, init: () -> T): T {
  getScopedValueOrNull<T>(key)?.let { return it }
  withLock {
    getScopedValueOrNull<T>(key)?.let { return it }
    val value = init()
    setScopedValue(key, value)
    return value
  }
}

inline fun <T : Any> Scope.getOrCreateScopedValue(key: TypeKey<T>, init: () -> T): T =
  getOrCreateScopedValue(key.value, init)

inline fun <@ForTypeKey T : Any> Scope.getOrCreateScopedValue(init: () -> T): T =
  getOrCreateScopedValue(typeKeyOf(), init)

/**
 * Invokes the [action] function once [this] scope get's disposed
 * or invokes it synchronously if [this] is already disposed
 *
 * Returns a [ScopeDisposable] to unregister the [action]
 */
fun Scope.invokeOnDispose(action: () -> Unit): ScopeDisposable {
  if (isDisposed) {
    action()
    return NoOpScopeDisposable
  }
  withLock {
    if (isDisposed) {
      action()
      return NoOpScopeDisposable
    }
    val key = InvokeOnDisposeKey()
    var notifyDisposal = true
    setScopedValue(key, ScopeDisposable {
      if (notifyDisposal) action()
    })
    return ScopeDisposable {
      notifyDisposal = false
      removeScopedValue(key)
    }
  }
}

inline fun <R> Scope.withLock(block: () -> R): R = synchronized(this, block)

private class InvokeOnDisposeKey

private val NoOpScopeDisposable = ScopeDisposable { }

class ScopeElement<S : Scope>(val key: TypeKey<*>, val factory: () -> Any)

/**
 * Registers the declaration a element in the [Scope] [S]
 *
 * Example:
 * ```
 * @InstallElement<AppScope>
 * @Provide
 * class MyAppDeps(val api: Api, val database: Database)
 *
 * fun runApp(@Inject appScope: AppScope) {
 *   val deps = appScope.element<MyAppDeps>()
 * }
 * ```
 */
@Qualifier annotation class InstallElement<S : Scope> {
  companion object {
    @Provide class Module<@Spread T : @InstallElement<S> U, U : Any, S : Scope> {
      @Provide inline fun scopeElement(
        noinline factory: () -> T,
        key: @Private TypeKey<U>
      ): ScopeElement<S> = ScopeElement(key, factory)

      @Provide inline fun elementAccessor(scope: S, key: @Private TypeKey<U>): U = scope.element(key)
    }

    @Qualifier private annotation class Private

    @Provide inline fun <@ForTypeKey T> elementTypeKey(): @Private TypeKey<T> = typeKeyOf()
  }
}

/**
 * Will get invoked once [Scope] [S] is initialized
 *
 * Example:
 * ```
 * @Provide fun imageLoaderInitializer(app: App): ScopeInitializer<AppScope> = {
 *   ImageLoader.init(app)
 * }
 * ```
 */
typealias ScopeInitializer<S> = () -> Unit

@PublishedApi internal class ScopeImpl(
  override val typeKey: TypeKey<Scope>,
  override val parent: Scope?
) : Scope {
  private var _isDisposed = false
  override val isDisposed: Boolean
    get() {
      if (_isDisposed) return true
      return synchronized(this) { _isDisposed }
    }

  lateinit var elements: Map<String, () -> Any>
  private var _scopedValues: MutableMap<Any, Any>? = null
  private fun scopedValues(): MutableMap<Any, Any> =
    (_scopedValues ?: hashMapOf<Any, Any>().also { _scopedValues = it })

  override fun <T> elementOrNull(key: TypeKey<T>): T? =
    elements[key.value]?.invoke() as? T ?: parent?.elementOrNull(key)

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> getScopedValueOrNull(key: Any): T? = _scopedValues?.get(key) as? T

  override fun <T : Any> setScopedValue(key: Any, value: T) {
    synchronizedWithDisposedCheck {
      removeScopedValueImpl(key)
      scopedValues()[key] = value
    } ?: kotlin.run {
      (value as? ScopeDisposable)?.dispose()
    }
  }

  override fun removeScopedValue(key: Any) {
    synchronizedWithDisposedCheck { removeScopedValueImpl(key) }
  }

  override fun dispose() {
    synchronizedWithDisposedCheck {
      _isDisposed = true
      if (_scopedValues != null && _scopedValues!!.isNotEmpty()) {
        _scopedValues!!.keys
          .toList()
          .forEach { removeScopedValueImpl(it) }
      }
    }
  }

  private fun removeScopedValueImpl(key: Any) {
    (_scopedValues?.remove(key) as? ScopeDisposable)?.dispose()
  }

  private inline fun <R> synchronizedWithDisposedCheck(block: () -> R): R? {
    if (_isDisposed) return null
    synchronized(this) {
      if (_isDisposed) return null
      return block()
    }
  }
}
