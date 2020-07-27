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
import kotlin.reflect.KClass

class ReaderContextBuilder(
    val name: KClass<*>?,
    val parent: ReaderContext?
) {

    private val elements: MutableMap<ReaderContext.Key<*>, Any> = ConcurrentHashMap()

    private val elementFactories = mutableListOf<ElementFactory>()

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

    fun elementFactory(factory: ElementFactory) {
        elementFactories += factory
    }

    fun build(): ReaderContext = ReaderContext(name, parent, elements, elementFactories)

}

inline fun readerContext(
    name: KClass<*>? = null,
    parent: ReaderContext? = null,
    block: ReaderContextBuilder.() -> Unit = {}
): ReaderContext = ReaderContextBuilder(name, parent).apply(block).build()
