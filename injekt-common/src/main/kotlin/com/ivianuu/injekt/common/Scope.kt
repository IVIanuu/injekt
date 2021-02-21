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
import com.ivianuu.injekt.Macro
import com.ivianuu.injekt.Qualifier

interface Scope {
    operator fun <T : Any> get(key: Int): T?
    operator fun <T : Any> set(key: Int, value: T)
    operator fun <T : Any> minusAssign(key: Int)
    fun dispose()
    interface Disposable {
        fun dispose()
    }
}

fun Scope(): Scope = ScopeImpl()

@Qualifier annotation class Scoped<S : Scope>

@Macro
@Given
inline fun <@ForTypeKey T : @Scoped<U> S, @ForTypeKey S : Any, @ForTypeKey U : Scope> scopedImpl(
    @Given scope: U,
    @Given factory: () -> T
): S = scope(factory)

inline operator fun <T : Any> Scope.invoke(key: Int, block: () -> T): T {
    get<T>(key)?.let { return it }
    synchronized(this) {
        get<T>(key)?.let { return it }
        val value = block()
        set(key, value)
        return value
    }
}

inline operator fun <T : Any> Scope.invoke(key: Any, block: () -> T): T =
    this(key.hashCode(), block)

inline operator fun <@ForTypeKey T : Any> Scope.invoke(block: () -> T): T =
    this(typeKeyOf<T>(), block)

private class ScopeImpl(private val values: MutableMap<Int, Any> = mutableMapOf()) : Scope {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Int): T? = values[key] as? T

    override fun <T : Any> set(key: Int, value: T) {
        values[key] = value
    }

    override fun <T : Any> minusAssign(key: Int) {
        values -= key
    }

    override fun dispose() {
        values.values
            .filterIsInstance<Scope.Disposable>()
            .forEach { it.dispose() }
        values.clear()
    }
}
