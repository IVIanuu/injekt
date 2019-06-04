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

/**
 * Attributes for [Binding]s
 */
/* inline */class Attributes(private val _entries: MutableMap<String, Any?> = mutableMapOf()) {

    val entries: Map<String, Any?> get() = _entries

    fun contains(key: String): Boolean = _entries.contains(key)

    operator fun <T> set(key: String, value: T) {
        _entries[key] = value as Any
    }

    fun <T> getOrNull(key: String): T? = _entries[key] as? T

    override fun toString(): String = entries.toString()

}

operator fun <T> Attributes.get(key: String): T =
    getOrNull<T>(key) ?: error("Couldn't get attribute for $key")

inline fun <T> Attributes.getOrSet(key: String, defaultValue: () -> T): T {
    val value = getOrNull<T>(key)

    if (value == null) {
        val def = defaultValue()
        set(key, def)
        return def
    }

    return value
}

inline fun <T> Attributes.getOrDefault(key: String, defaultValue: () -> T): T =
    getOrNull<T>(key) ?: defaultValue()

fun attributesOf(): Attributes =
    Attributes(mutableMapOf())

fun attributesOf(vararg pairs: Pair<String, Any?>): Attributes =
    Attributes(mutableMapOf(*pairs))