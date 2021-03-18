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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier

interface Scope : ScopeDisposable {
    /**
     * Whether or not this scope is disposed
     */
    val isDisposed: Boolean
    /**
     * Returns the value [T] for [key] or null
     */
    fun <T : Any> get(key: Any): T?
    /**
     * Sets the value [T] for [key] to [value]
     */
    fun <T : Any> set(key: Any, value: T)
    /**
     * Removes the value for [key]
     */
    fun remove(key: Any)
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

/**
 * Returns a new [Scope] instance
 */
fun Scope(): Scope = ScopeImpl(mutableMapOf())

/**
 * Converts a [@Scoped<S> T] to a [T] which is scoped to the lifecycle of [S]
 *
 * In the following example each request to Repo resolvers to the same instance
 * ```
 * @Scoped<AppComponent>
 * @Given
 * class MyRepo
 *
 * fun runApp(@Given appComponent: AppComponent) {
 *     val repo1 = given<MyRepo>()
 *     val repo2 = given<MyRepo>()
 *     // repo === repo2
 * }
 *
 * ```
 */
@Qualifier
annotation class Scoped<S : Scope>

@Given
inline fun <@Given T : @Scoped<U> S, @ForTypeKey S : Any, U : Scope> scopedImpl(
    @Given scope: U,
    @Given factory: () -> T
): S = scope.getOrCreate<S>(factory)

/**
 * Returns an existing instance of [T] for key [key] or creates and caches a new instance by calling function [init]
 */
inline fun <T : Any> Scope.getOrCreate(key: Any, init: () -> T): T {
    get<T>(key)?.let { return it }
    synchronized(this) {
        get<T>(key)?.let { return it }
        val value = init()
        set(key, value)
        return value
    }
}

inline fun <@ForTypeKey T : Any> Scope.getOrCreate(block: () -> T): T = getOrCreate(typeKeyOf<T>(), block)

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
    synchronized(this) {
        if (isDisposed) {
            action()
            return NoOpScopeDisposable
        }
        val key = InvokeOnDisposeKey()
        var notifyDisposal = true
        set(key, ScopeDisposable {
            if (notifyDisposal) action()
        })
        return ScopeDisposable {
            notifyDisposal = false
            remove(key)
        }
    }
}

private class InvokeOnDisposeKey

private val NoOpScopeDisposable = ScopeDisposable {  }

private class ScopeImpl(private val values: MutableMap<Any, Any>) : Scope {
    override var isDisposed = false

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Any): T? {
        if (isDisposed) return null
        return values[key] as? T
    }

    override fun <T : Any> set(key: Any, value: T) {
        if (isDisposed) return
        remove(key)
        values[key] = value
    }

    override fun remove(key: Any) {
        if (isDisposed) return
        removeImpl(key)
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        if (values.isNotEmpty()) {
            values.keys
                .toList()
                .forEach { removeImpl(it) }
        }
    }

    private fun removeImpl(key: Any) {
        (values.remove(key) as? ScopeDisposable)?.dispose()
    }
}
