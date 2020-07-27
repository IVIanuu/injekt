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

    private val map = ConcurrentHashMap(
        base?.backing ?: emptyMap()
    )

    private val justInTimeValueProviders = mutableListOf<JustInTimeValueProvider>()

    init {
        Injekt.builderInterceptors.forEach { it(this) }
    }

    operator fun <T : Any> set(
        key: ReaderContext.Key<T>,
        value: T
    ) {
        map[key] = (if (map.containsKey(key)) key.merge(map[key]!! as T, value) else value) as Any
    }

    fun justInTimeValueProvider(justInTimeValueProvider: JustInTimeValueProvider) {
        justInTimeValueProviders += justInTimeValueProvider
    }

    fun build(): ReaderContext {
        map[JustInTimeValueProvidersKey] = justInTimeValueProviders
        return ReaderContext(map)
    }

}

inline fun readerContext(
    base: ReaderContext? = null,
    block: ReaderContextBuilder.() -> Unit = {}
): ReaderContext = ReaderContextBuilder(base).apply(block).build()
