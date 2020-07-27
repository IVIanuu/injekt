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

package com.ivianuu.injekt

import kotlin.reflect.KClass

class ReaderContext(val backing: MutableMap<Key<*>, Any>) {

    operator fun contains(key: Key<*>): Boolean = key in backing

    operator fun <T : Any> get(key: Key<T>): T {
        var value = getOrNull(key)
        if (value == null) {
            value = key.fallback()
        }
        return value
    }

    operator fun <T : Any> set(key: Key<T>, value: T) {
        backing[key] = value
    }

    inline fun <T : Any> getOrSet(
        key: Key<T>,
        init: () -> T
    ): T = getOrNull(key) ?: init().also { this[key] = it }

    fun <T : Any> getOrNull(key: Key<T>): T? {
        var value = backing[key] as? T?

        if (value == null) {
            val justInTimeValueProviders = getOrNull(JustInTimeValueProvidersKey)
            if (justInTimeValueProviders != null) {
                for (provider in justInTimeValueProviders) {
                    val result = withReaderContext(this) {
                        provider.get(key)
                    }
                    if (result != null) {
                        value = result.value
                        break
                    }
                }
            }
        }

        return value
    }

    interface Key<T : Any> {

        fun merge(
            oldValue: T,
            newValue: T
        ): T = newValue

        fun fallback(): T = error("No value provided for $this")

    }

}

typealias ReaderContextBuilderInterceptor = ReaderContextBuilder.() -> Unit

@Reader
fun <T : Any> getFactory(
    key: TypeKey<T>
): @Reader (Arguments) -> T {
    val factoryKey = FactoryKey(key)
    return readerContext[factoryKey]
}

data class FactoryKey<T : Any>(
    val typeKey: TypeKey<T>
) : ReaderContext.Key<@Reader (Arguments) -> T>

inline fun <reified T : Any> factoryKeyOf() = FactoryKey<T>(typeKeyOf())

data class TypeKey<T : Any>(val value: KClass<*>) : ReaderContext.Key<T>

inline fun <reified T : Any> typeKeyOf() = TypeKey<T>(T::class)

@Reader
val readerContext: ReaderContext
    get() = injektIntrinsic()

inline fun <R> withReaderContext(
    context: ReaderContext,
    block: @Reader () -> R
): R = injektIntrinsic()
