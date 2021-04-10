/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.common.ForTypeKey
import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.common.typeKeyOf

/**
 * A hierarchical construct with a lifecycle which hosts a map of keys to elements
 * which can be retrieved via [GivenScope.element]
 */
interface GivenScope : GivenScopeDisposable {
    /**
     * Whether or not this scope is disposed
     */
    val isDisposed: Boolean
    /**
     * Returns the element [T] or throws
     */
    fun <T : Any> element(key: TypeKey<T>): T
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
}

/**
 * Allows scoped values to be notified when the hosting [GivenScope] get's disposed
 */
fun interface GivenScopeDisposable {
    /**
     * Get's called while the hosting [GivenScope] get's disposed via [GivenScope.dispose]
     */
    fun dispose()
}

fun <@ForTypeKey T : Any> GivenScope.element(): T = element(typeKeyOf())

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [init]
 */
inline fun <T : Any> GivenScope.getOrCreateScopedValue(key: Any, init: () -> T): T {
    getScopedValueOrNull<T>(key)?.let { return it }
    withLock {
        getScopedValueOrNull<T>(key)?.let { return it }
        val value = init()
        setScopedValue(key, value)
        return value
    }
}

inline fun <@ForTypeKey T : Any> GivenScope.getOrCreateScopedValue(init: () -> T): T =
    getOrCreateScopedValue(typeKeyOf(), init)

inline fun <T : Any> GivenScope.getOrCreateScopedValue(key: TypeKey<T>, init: () -> T): T =
    getOrCreateScopedValue(key.value, init)

/**
 * Invokes the [action] function once [this] scope get's disposed
 * or invokes it synchronously if [this] is already disposed
 *
 * Returns a [GivenScopeDisposable] to unregister the [action]
 */
fun GivenScope.invokeOnDispose(action: () -> Unit): GivenScopeDisposable {
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
        setScopedValue(key, GivenScopeDisposable {
            if (notifyDisposal) action()
        })
        return GivenScopeDisposable {
            notifyDisposal = false
            removeScopedValue(key)
        }
    }
}

inline fun <R> GivenScope.withLock(block: () -> R): R = synchronized(this, block)

private class InvokeOnDisposeKey

private val NoOpScopeDisposable = GivenScopeDisposable {  }

typealias DefaultGivenScope = GivenScope

@Given
inline fun <S : DefaultGivenScope> defaultGivenScope(
    @Given elements: (@Given S) -> Set<GivenScopeElement<S>> = { emptySet() },
    @Given initializers: (@Given S) -> Set<GivenScopeInitializer<S>> = { emptySet() }
): S {
    val scope = GivenScopeImpl()
    scope as S
    val finalElements = elements(scope)
    scope.elements = if (finalElements.isEmpty()) emptyMap()
    else HashMap<String, () -> Any>(finalElements.size).apply {
        finalElements.forEach { this[it.key.value] = it.factory }
    }
    initializers(scope).forEach { it() }
    return scope
}

@Suppress("unused")
data class GivenScopeElement<S : GivenScope>(val key: TypeKey<*>, val factory: () -> Any)

/**
 * Registers the declaration a element in the [GivenScope] [S]
 *
 * Example:
 * ```
 * @GivenScopeElementBinding<AppGivenScope>
 * @Given
 * class MyAppDeps(@Given api: Api, @Given database: Database)
 *
 * fun runApp(@Given appScope: AppGivenScope) {
 *    val deps = appScope.element<MyAppDeps>()
 * }
 * ```
 */
@Qualifier
annotation class InstallElement<S : GivenScope>

@Given
class GivenScopeElementModule<@Given T : @InstallElement<S> U, U : Any, S : GivenScope> {
    @Given
    fun elementIntoSet(
        @Given factory: () -> T,
        @Given key: TypeKey<U>
    ): GivenScopeElement<S> = GivenScopeElement(key, factory)

    @Given
    fun element(@Given scope: S, @Given key: TypeKey<U>): U = scope.element(key)
}

/**
 * Will get invoked once [GivenScope] [S] is initialized
 *
 * Example:
 * ```
 * @Given
 * fun imageLoaderInitializer(@Given app: App): GivenScopeInitializer<AppGivenScope> = {
 *     ImageLoader.init(app)
 * }
 * ```
 */
typealias GivenScopeInitializer<@Suppress("unused", "UNUSED_TYPEALIAS_PARAMETER") S> = () -> Unit

@PublishedApi
internal class GivenScopeImpl : GivenScope {
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

    override fun <T : Any> element(key: TypeKey<T>): T = elements[key.value]?.invoke() as? T
        ?: error("No element found for $key in $this")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getScopedValueOrNull(key: Any): T? = _scopedValues?.get(key) as? T

    override fun <T : Any> setScopedValue(key: Any, value: T) {
        synchronizedWithDisposedCheck {
            removeScopedValueImpl(key)
            scopedValues()[key] = value
        } ?: kotlin.run {
            (value as? GivenScopeDisposable)?.dispose()
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
        (_scopedValues?.remove(key) as? GivenScopeDisposable)?.dispose()
    }

    private inline fun <R> synchronizedWithDisposedCheck(block: () -> R): R? {
        if (_isDisposed) return null
        synchronized(this) {
            if (_isDisposed) return null
            return block()
        }
    }
}
