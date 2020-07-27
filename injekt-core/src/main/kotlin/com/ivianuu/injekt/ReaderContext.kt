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

class ReaderContext internal constructor(
    val name: KClass<*>?,
    val parent: ReaderContext?,
    val elements: MutableMap<Key<*>, Any>,
    val elementFactories: List<ElementFactory>
) {

    operator fun <T : Any> get(key: Key<T>): T {
        var value = getOrNull(key)
        if (value == null) {
            value = parent?.getOrNull(key)
            if (value == null) {
                value = key.fallback()
            }
        }
        return value
    }

    operator fun <T : Any> set(key: Key<T>, value: T) {
        elements[key] = value
    }

    inline fun <T : Any> getOrSet(
        key: Key<T>,
        init: () -> T
    ): T = getOrNull(key) ?: init().also { this[key] = it }

    fun <T : Any> getOrNull(key: Key<T>): T? {
        elements[key]?.let { return it as T }

        for (factory in elementFactories) {
            val element = withReaderContext(this) { factory(key) }
            if (element != null) return element as T
        }

        return null
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
    key: FactoryKey<T>
): @Reader (Arguments) -> T = readerContext[key]

class FactoryKey<T : Any>(
    val type: KClass<*>
) : ReaderContext.Key<@Reader (Arguments) -> T> {
    private val hashCode = type.hashCode()
    override fun hashCode(): Int = hashCode
    override fun equals(other: Any?): Boolean =
        other is FactoryKey<*> && other.hashCode == hashCode
}

inline fun <reified T : Any> factoryKeyOf() = FactoryKey<T>(T::class)

@Reader
val readerContext: ReaderContext
    get() = injektIntrinsic()

inline fun <R> withReaderContext(
    context: ReaderContext,
    block: @Reader () -> R
): R = injektIntrinsic()
