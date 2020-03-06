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

package com.ivianuu.injekt.comparison.container.impl

class Properties internal constructor(val entries: Map<Any, Any?>) {

    operator fun contains(key: Any): Boolean = key in entries

    operator fun <T> get(key: Any): T {
        check(key in entries) { "No value for key $key" }
        return entries[key] as T
    }

    fun <T> getOrNull(key: Any): T? = entries[key] as? T

    inline fun copy(block: PropertiesBuilderBlock): Properties =
        propertiesOf(entries.toMutableMap().apply(block))

    override fun toString(): String = entries.toString()

}

typealias PropertiesBuilderBlock = MutableMap<Any, Any?>.() -> Unit

private val emptyProperties = Properties(emptyMap())

fun emptyProperties(): Properties = emptyProperties

fun propertiesOf(vararg pairs: Pair<Any, Any?>): Properties = propertiesOf(pairs.toMap())

fun propertiesOf(entries: Map<Any, Any?>): Properties = Properties(entries)

inline fun propertiesOf(block: PropertiesBuilderBlock) = propertiesOf(mutableMapOf<Any, Any?>().apply(block))
