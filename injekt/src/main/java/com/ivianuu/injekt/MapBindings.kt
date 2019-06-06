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

class MapBindings {

    private val maps = mutableMapOf<Key, BindingMap<*, *>>()

    fun putAll(mapBindings: MapBindings) {
        mapBindings.maps.forEach { (mapKey, map) ->
            val thisMap = get<Any?, Any?>(mapKey)
            thisMap.putAll(map as BindingMap<Any?, Any?>)
        }
    }

    fun putIfAbsent(mapKey: Key) {
        maps.getOrPut(mapKey) { BindingMap<Any?, Any?>(mapKey) }
    }

    fun <K, V> get(mapKey: Key): BindingMap<K, V> {
        return maps.getOrPut(mapKey) { BindingMap<K, V>(mapKey) } as BindingMap<K, V>
    }

    fun getAll(): Map<Key, BindingMap<*, *>> = maps

    class BindingMap<K, V> internal constructor(private val mapKey: Key) {
        private val map = mutableMapOf<K, Entry<V>>()

        fun putAll(other: BindingMap<K, V>) {
            other.map.forEach { (key, entry) -> put(key, entry) }
        }

        fun put(key: K, binding: Binding<V>, override: Boolean) {
            put(key, Entry(binding, override))
        }

        private fun put(key: K, entry: Entry<V>) {
            if (map.contains(key) && !entry.override) {
                error("Already declared $key in map $mapKey")
            }

            map[key] = entry
        }

        fun getBindingMap(): Map<K, Binding<V>> = map.mapValues { it.value.binding }

        private class Entry<V>(
            val binding: Binding<V>,
            val override: Boolean
        )
    }
}