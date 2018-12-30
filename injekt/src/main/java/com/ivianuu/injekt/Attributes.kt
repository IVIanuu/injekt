/*
 * Copyright 2018 Manuel Wrage
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

/**
 * Attributes for [Declaration]s
 */
data class Attributes(private val data: MutableMap<String, Any> = ConcurrentHashMap()) {

    fun contains(key: String) = data.contains(key)

    operator fun <T> set(key: String, value: T) {
        data[key] = value as Any
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String) = data[key] as? T

}

inline fun <T> Attributes.getOrSet(key: String, defaultValue: () -> T): T {
    val value = get<T>(key)

    if (value == null) {
        val def = defaultValue()
        set(key, def)
        return def
    }

    return value
}