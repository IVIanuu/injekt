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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.ForTypeKey
import com.ivianuu.injekt.common.typeKeyOf

interface GivenScope : GivenScopeDisposable {
    /**
     * Whether or not this scope is disposed
     */
    val isDisposed: Boolean
    /**
     * Returns the element [T] for [key] or null
     */
    fun <T : Any> getElementOrNull(key: Any): T?
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
 * Returns a new [GivenScope] instance
 */
@Suppress("UNCHECKED_CAST")
fun <S : GivenScope> GivenScope(
    @Given elements: (@Given S) -> Set<GivenScopeElement<S>>,
    @Given initializers: (@Given S) -> Set<GivenScopeInitializer<S>>
): S {
    val scope = GivenScopeImpl()
    elements(scope as S).forEach { scope.elements[it.key] = it.factory }
    initializers(scope).forEach { it(scope) }
    return scope
}

/**
 * Returns the element of [T] for [key] or throws
 */
fun <T : Any> GivenScope.getElement(key: Any): T =
    getElementOrNull(key) ?: error("No element found for $key")

fun <@ForTypeKey T : Any> GivenScope.getElement(): T = getElement(typeKeyOf<T>())

/**
 * Returns an existing instance of [T] for [key] or creates and caches a new instance by calling function [init]
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

@Suppress("UNCHECKED_CAST")
private class GivenScopeImpl : GivenScope {
    val elements = mutableMapOf<Any, () -> Any>()
    val scopedValues = mutableMapOf<Any, Any>()

    override var isDisposed = false

    override fun <T : Any> getElementOrNull(key: Any): T? {
        if (isDisposed) return null
        return elements[key]?.invoke() as? T
    }

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
        scopedValues.keys
            .forEach { removeImpl(it) }
        elements.clear()
    }

    private fun removeImpl(key: Any) {
        (scopedValues.remove(key) as? GivenScopeDisposable)?.dispose()
    }
}
