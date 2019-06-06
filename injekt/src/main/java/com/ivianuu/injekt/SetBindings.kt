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

class SetBindings {

    private val sets = hashMapOf<Key, BindingSet<*>>()

    fun putAll(setBindings: SetBindings) {
        setBindings.sets.forEach { (setKey, set) ->
            val thisSet = get<Any?>(setKey)
            thisSet.putAll(set as BindingSet<Any?>)
        }
    }

    fun putIfAbsent(setKey: Key) {
        sets.getOrPut(setKey) { BindingSet<Any?>(setKey) }
    }

    fun <E> get(setKey: Key): BindingSet<E> {
        return sets.getOrPut(setKey) { BindingSet<Any?>(setKey) } as BindingSet<E>
    }

    fun getAll(): Map<Key, BindingSet<*>> = sets

    class BindingSet<E> internal constructor(private val setKey: Key) {

        private val map = linkedMapOf<Key, Boolean>()

        fun putAll(other: BindingSet<E>) {
            other.map.forEach { (key, override) -> put(key, override) }
        }

        private fun put(elementKey: Key, override: Boolean) {
            if (map.contains(elementKey) && !override) {
                error("Already declared $elementKey in set $setKey")
            }

            map[elementKey] = override
        }

        fun getBindingSet(): Set<Key> = map.keys

    }
}