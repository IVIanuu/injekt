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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.TypeKey

interface Scope : Disposable {
  /**
   * Whether or not this scope is disposed
   */
  val isDisposed: Boolean

  /**
   * Returns the scoped value [T] for [key] or null
   */
  @InternalScopeApi fun <T> getScopedValueOrNull(key: Any): T?

  /**
   * Store's [value] for [key]
   *
   * If [value] is a [Disposable] [Disposable.dispose] will be invoked once this scope gets disposed
   */
  @InternalScopeApi fun <T> setScopedValue(key: Any, value: T)

  /**
   * Removes the scoped value for [key]
   */
  @InternalScopeApi fun removeScopedValue(key: Any)

  /**
   * All elements of this scope
   */
  @InternalScopeApi val elements: Map<String, () -> Any?>
}

/**
 * Returns the element of [T] for [key] in [scope] if exists or null
 */
@OptIn(InternalScopeApi::class)
fun <T> elementOrNull(@Inject scope: Scope, @Inject key: TypeKey<T>): T? =
  scope.elements[key.value]?.invoke() as? T

@Tag annotation class DefaultElementValue

/**
 * Returns the element of [T] for [key] in [scope] if exists or the result [defaultValue]
 */
inline fun <T> element(
  @Inject scope: Scope,
  @Inject key: TypeKey<T>,
  @Inject defaultValue: () -> @DefaultElementValue T
): T = elementOrNull(scope, key) ?: defaultValue()

/**
 * Returns the element of [T] for [key] in [scope] if exists or throws
 */
@OptIn(InternalScopeApi::class)
fun <T> requireElement(@Inject scope: Scope, @Inject key: TypeKey<T>): T {
  val factory = scope.elements[key.value]
  checkNotNull(factory) { "No element provided for $key" }
  return factory() as T
}

class ProvidedElement<T> internal constructor(
  @Inject val key: TypeKey<T>,
  @Inject val merge: MergeElement<T>,
  val factory: () -> T
)

fun <T> provideElement(
  @Inject key: TypeKey<T>,
  @Inject merge: MergeElement<T>,
  factory: () -> T
) = ProvidedElement(key, merge, factory)

fun <T> provideDefaultElement(
  @Inject key: TypeKey<T>,
  factory: () -> T
) = ProvidedElement(key, { old, new -> old?.invoke() ?: new() }, factory)

typealias MergeElement<T> = ((() -> T)?, () -> T) -> T

private val OverrideMergeElement: MergeElement<Any?> = { _, new -> new }

@Provide fun <T> mergeElement(): MergeElement<T> = OverrideMergeElement as MergeElement<T>

private fun mergeElements(
  oldElements: Map<String, () -> Any?>,
  newElements: Array<out ProvidedElement<*>>
): Map<String, () -> Any?> {
  if (newElements.isEmpty()) return oldElements

  val newMap = oldElements.toMutableMap()

  for (newValue in newElements) {
    val oldFactory = newMap[newValue.key.value]
    newMap[newValue.key.value] = when {
      newValue.merge === OverrideMergeElement -> newValue.factory
      else -> {
        {
          (newValue.merge as MergeElement<Any?>)
            .invoke(oldFactory, newValue.factory)
        }
      }
    }
  }

  return newMap
}

@RequiresOptIn annotation class InternalScopeApi

@OptIn(InternalScopeApi::class)
fun childScope(
  vararg elements: ProvidedElement<*>,
  @Inject parentScope: Scope
): Scope = ScopeImpl(mergeElements(parentScope.elements, elements))
  .also { ParentScopeDisposable(it).bind(parentScope) }

/**
 * Runs the [block] with a fresh [Scope] which will be disposed after the execution
 */
inline fun <R> withScope(
  vararg elements: ProvidedElement<*>,
  block: (@Inject Scope) -> R
): R {
  @Provide val scope = scopeOf(*elements)
  return scope.use { block() }
}

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [computation]
 */
@OptIn(InternalScopeApi::class)
inline fun <T> scoped(key: Any, @Inject scope: Scope, computation: () -> T): T {
  scope.getScopedValueOrNull<T>(key)?.let { return it }
  synchronized(scope) {
    scope.getScopedValueOrNull<T>(key)?.let { return it }
    val value = computation()
    scope.setScopedValue(key, value)
    return value
  }
}

inline fun <T> scoped(@Inject key: TypeKey<T>, @Inject scope: Scope, computation: () -> T): T =
  scoped(key = key.value, computation = computation)

@OptIn(InternalScopeApi::class)
inline fun <T> scoped(
  key: Any,
  vararg args: Any?,
  @Inject scope: Scope,
  computation: () -> T
): T {
  val holder = scoped(key) { ScopedValueHolder() }

  synchronized(holder) {
    if (!args.contentEquals(holder.args)) {
      (holder.value as? Disposable)?.dispose()
      holder.value = computation()
      holder.args = args
      (holder.value as? ScopeObserver)?.init()
    }
  }

  return holder.value as T
}

@PublishedApi internal class ScopedValueHolder : Disposable {
  var value: Any? = null
  var args: Array<out Any?>? = null

  override fun dispose() {
    args = null
    (value as? Disposable)?.dispose()
    value = null
  }
}

/**
 * Invokes the [action] function if once for [key] in [scope]
 */
fun onInit(key: Any, @Inject scope: Scope, action: () -> Unit) {
  data class OnInitKey(val key: Any)
  scoped(OnInitKey(key)) { action() }
}

/**
 * Invokes the [action] function once [scope] gets disposed
 * or invokes it synchronously if [scope] is already disposed
 */
inline fun onDispose(@Inject scope: Scope, crossinline action: () -> Unit): Disposable =
  Disposable { action() }.bind()

/**
 * Runs the [block] and disposes [this] afterwards using [disposer]
 */
inline fun <T, R> T.use(@Inject disposer: Disposer<T>, block: () -> R): R = try {
  block()
} finally {
  disposer.dispose(this)
}


/**
 * Lifecycle observer for [Scope]
 */
interface ScopeObserver : Disposable {
  /**
   * Will be called once the observer is attached to a [Scope]
   */
  fun init() {
  }

  override fun dispose() {
  }
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

private object NoopDisposable : Disposable {
  override fun dispose() {
  }
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

@Provide fun <T : Disposable> disposableDisposer() = Disposer<T> { it.dispose() }

/**
 * Returns a [Disposable] which disposes [this] using [disposer]
 */
fun <T> T.asDisposable(@Inject disposer: Disposer<T>): Disposable =
  Disposable { disposer.dispose(this) }

/**
 * Disposes this value with [scope] using [disposer]
 */
@OptIn(InternalScopeApi::class)
fun <T> T.bind(@Inject scope: Scope, @Inject disposer: Disposer<T>): Disposable {
  if (scope.isDisposed) {
    disposer.dispose(this)
    return NoopDisposable
  }

  synchronized(scope) {
    if (scope.isDisposed) {
      disposer.dispose(this)
      return NoopDisposable
    }

    var callDispose = true
    val disposable = Disposable {
      if (callDispose) {
        callDispose = false
        disposer.dispose(this)
      }
    }

    scope.setScopedValue(disposable, disposable)
    val innerDisposable = Disposable {
      callDispose = false
      scope.removeScopedValue(disposable)
    }
    return innerDisposable
  }
}

/**
 * Returns a new [Scope]
 */
@OptIn(InternalScopeApi::class)
fun scopeOf(): Scope = ScopeImpl(emptyMap())

/**
 * Returns a new [Scope] with [elements]
 */
@OptIn(InternalScopeApi::class)
fun scopeOf(vararg elements: ProvidedElement<*>): Scope =
  ScopeImpl(mergeElements(emptyMap(), elements))

@InternalScopeApi internal class ScopeImpl(
  override val elements: Map<String, () -> Any?>
) : Scope {
  private var _isDisposed = false
  override val isDisposed: Boolean
    get() {
      if (_isDisposed) return true
      return synchronized(this) { _isDisposed }
    }

  private val scopedValues = hashMapOf<Any, Any?>()

  @Suppress("UNCHECKED_CAST")
  override fun <T> getScopedValueOrNull(key: Any): T? = scopedValues[key] as? T

  override fun <T> setScopedValue(key: Any, value: T) {
    synchronizedWithDisposedCheck {
      removeAndDispose(key)
      scopedValues[key] = value
      (value as? ScopeObserver)?.init()
      Unit
    } ?: kotlin.run {
      (value as? Disposable)?.dispose()
    }
  }

  override fun removeScopedValue(key: Any) {
    synchronizedWithDisposedCheck { scopedValues.remove(key) }
      ?.let { (it as? Disposable)?.dispose() }
  }

  override fun dispose() {
    synchronizedWithDisposedCheck {
      _isDisposed = true
      if (scopedValues.isNotEmpty()) {
        scopedValues.keys
          .toList()
          .forEach { removeAndDispose(it) }
      }
    }
  }

  private fun removeAndDispose(key: Any) {
    (scopedValues.remove(key) as? Disposable)?.dispose()
  }

  private inline fun <R> synchronizedWithDisposedCheck(block: () -> R): R? {
    if (_isDisposed) return null
    synchronized(this) {
      if (_isDisposed) return null
      return block()
    }
  }
}
