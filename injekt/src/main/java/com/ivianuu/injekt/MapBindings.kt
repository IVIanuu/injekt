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

    private val maps = hashMapOf<Key, BindingMap<*, *>>()

    fun putAll(mapBindings: MapBindings) {
        mapBindings.maps.forEach { (mapKey, map) ->
            val thisMap = get<Any?, Any?>(mapKey)
            thisMap.putAll(map as BindingMap<Any?, Any?>)
        }
    }

    fun <K, V> get(mapKey: Key): BindingMap<K, V> {
        return maps.getOrPut(mapKey) { BindingMap<K, V>(mapKey) } as BindingMap<K, V>
    }

    fun getAll(): Map<Key, BindingMap<*, *>> = maps

    class BindingMap<K, V> internal constructor(private val mapKey: Key) {
        private val map = linkedMapOf<K, Entry>()

        fun putAll(other: BindingMap<K, V>) {
            other.map.forEach { (key, entry) -> put(key, entry) }
        }

        inline fun <reified T : V> put(
            entryKey: K,
            entryValueName: Any? = null,
            override: Boolean = false
        ) {
            put<T>(entryKey, typeOf(), entryValueName, override)
        }

        fun <T : V> put(
            entryKey: K,
            entryValueType: Type<T>,
            entryValueName: Any? = null,
            override: Boolean = false
        ) {
            val entryValueKey = keyOf(entryValueType, entryValueName)
            put(entryKey, entryValueKey, override)
        }

        fun put(
            entryKey: K,
            entryValueKey: Key,
            override: Boolean = false
        ) {
            put(entryKey, Entry(entryValueKey, override))
        }

        infix fun K.to(key: Key) {
            put(this, key)
        }

        infix fun K.to(type: Type<out V>) {
            put(this, keyOf(type))
        }

        private fun put(entryKey: K, entry: Entry) {
            if (map.contains(entryKey) && !entry.override) {
                error("Already declared $entryKey in map $mapKey")
            }

            map[entryKey] = entry
        }

        fun getBindingMap(): Map<K, Key> = map.mapValues { it.value.entryValueKey }

        private class Entry(
            val entryValueKey: Key,
            val override: Boolean
        )
    }
}