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
    fun <@ForTypeKey T : Any> element(): T
    /**
     * Returns the scoped value [T] for [key] or null
     */
    fun <T : Any> getScopedValueOrNull(key: Any): T?
    /**
     * Sets the scoped value [T] for [key] to [value]
     */
    fun <T : Any> setScopedValue(key: Any, value: T)
    /**
     * Removes the scoped value for [key]
     */
    fun removeScopedValue(key: Any)
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

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [init]
 */
inline fun <T : Any> GivenScope.getOrCreateScopedValue(key: Any, init: () -> T): T {
    getScopedValueOrNull<T>(key)?.let { return it }
    synchronized(this) {
        getScopedValueOrNull<T>(key)?.let { return it }
        val value = init()
        setScopedValue(key, value)
        return value
    }
}

inline fun <@ForTypeKey T : Any> GivenScope.getOrCreateScopedValue(block: () -> T): T =
    getOrCreateScopedValue(typeKeyOf<T>(), block)

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
    synchronized(this) {
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

private class InvokeOnDisposeKey

private val NoOpScopeDisposable = GivenScopeDisposable {  }

@Given
inline fun <@ForTypeKey S : GivenScope> givenScope(
    @Given elements: (@Given S) -> Set<GivenScopeElement<S>> = { emptySet() },
    @Given initializers: (@Given S) -> Set<GivenScopeInitializer<S>> = { emptySet() }
): S {
    val scope = GivenScopeImpl()
    scope as S
    elements(scope)
        .forEach { scope.elements[it.first] = it.second }
    initializers(scope).forEach { it() }
    return scope
}

typealias GivenScopeElement<@Suppress("unused", "UNUSED_TYPEALIAS_PARAMETER") S> = Pair<TypeKey<Any>, () -> Any>

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
annotation class GivenScopeElementBinding<S : GivenScope>

@Given
fun <@Given T : @GivenScopeElementBinding<U> S, @ForTypeKey S : Any, @ForTypeKey U : GivenScope>
        givenScopeElementBindingImpl(@Given factory: () -> T): GivenScopeElement<U> =
    typeKeyOf<S>() to factory as () -> Any

/**
 * Will get invoked once [GivenScope] [S] is initialized
 *
 * Example:
 * ```
 * @Given fun imageLoaderInitializer(@Given app: App): GivenScopeInitializer<AppGivenScope> = {
 *     ImageLoader.init(app)
 * }
 * ```
 */
typealias GivenScopeInitializer<@Suppress("unused", "UNUSED_TYPEALIAS_PARAMETER") S> = () -> Unit

@PublishedApi
internal class GivenScopeImpl : GivenScope {
    override var isDisposed = false

    val elements = mutableMapOf<TypeKey<*>, () -> Any>()
    private val scopedValues = mutableMapOf<Any, Any>()

    override fun <@ForTypeKey T : Any> element(): T {
        val key = typeKeyOf<T>()
        return elements[key]?.invoke() as? T
            ?: error("No element found for $key in $this")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getScopedValueOrNull(key: Any): T? {
        if (isDisposed) return null
        return scopedValues[key] as? T
    }

    override fun <T : Any> setScopedValue(key: Any, value: T) {
        if (isDisposed) return
        removeScopedValue(key)
        scopedValues[key] = value
    }

    override fun removeScopedValue(key: Any) {
        if (isDisposed) return
        removeImpl(key)
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        if (scopedValues.isNotEmpty()) {
            scopedValues.keys
                .toList()
                .forEach { removeImpl(it) }
        }
    }

    private fun removeImpl(key: Any) {
        (scopedValues.remove(key) as? GivenScopeDisposable)?.dispose()
    }
}
