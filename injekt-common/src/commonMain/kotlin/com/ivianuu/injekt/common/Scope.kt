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
     * Returns the value [T] for [key] or null
     */
    operator fun <T : Any> get(key: Any): T?

    /**
     * Sets the value [T] for [key] to [value]
     */
    operator fun <T : Any> set(key: Any, value: T)

    /**
     * Removes the value for [key]
     */
    operator fun minusAssign(key: Any)
}

/**
 * Allows scoped values to be notified when the hosting [Scope] get's disposed
 */
interface ScopeDisposable {
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
): S = scope<S>(factory)

/**
 * Returns an existing instance of [T] for key [key] or creates a new one by calling function [block]
 */
inline operator fun <T : Any> Scope.invoke(key: Any, block: () -> T): T {
    get<T>(key)?.let { return it }
    synchronized(this) {
        get<T>(key)?.let { return it }
        val value = block()
        set(key, value)
        return value
    }
}

inline operator fun <@ForTypeKey T : Any> Scope.invoke(block: () -> T): T =
    this(typeKeyOf<T>(), block)

private class ScopeImpl(private val values: MutableMap<Any, Any>) : Scope {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Any): T? = values[key] as? T

    override fun <T : Any> set(key: Any, value: T) {
        values[key] = value
    }

    override fun minusAssign(key: Any) {
        values -= key
    }

    override fun dispose() {
        values.values
            .filterIsInstance<ScopeDisposable>()
            .forEach { it.dispose() }
        values.clear()
    }
}
