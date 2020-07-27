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

import java.util.concurrent.ConcurrentHashMap

class ReaderContextBuilder(
    val base: ReaderContext? = null
) {

    private val elements: MutableMap<ReaderContext.Key<*>, Any> = ConcurrentHashMap(
        base?.elements ?: emptyMap()
    )

    private val lazyElementProviders = mutableListOf<LazyElementProvider>()

    init {
        Injekt.builderInterceptors.forEach { it(this) }
    }

    operator fun <T : Any> set(
        key: ReaderContext.Key<T>,
        value: T
    ) {
        elements[key] = (if (elements.containsKey(key)) key.merge(
            elements[key]!! as T,
            value
        ) else value) as Any
    }

    fun <T : Any> setOnce(key: ReaderContext.Key<T>, value: () -> T) {
        if (key !in elements) elements[key] = value
    }

    fun lazyElementProvider(lazyElementProvider: LazyElementProvider) {
        lazyElementProviders += lazyElementProvider
    }

    fun build(): ReaderContext = ReaderContext(elements, lazyElementProviders)

}

inline fun readerContext(
    base: ReaderContext? = null,
    block: ReaderContextBuilder.() -> Unit = {}
): ReaderContext = ReaderContextBuilder(base).apply(block).build()
