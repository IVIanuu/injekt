/*
 * Copyright 2019 Manuel Wrage
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

/*inline */ class SetBindings {

    private val sets: MutableMap<Key, BindingSet<*>> = hashMapOf()

    fun addAll(setBindings: SetBindings) {
        setBindings.sets.forEach { (setKey, set) ->
            val thisSet = get<Any?>(setKey)
            thisSet.addAll(set as BindingSet<Any?>)
        }
    }

    fun <E> get(setKey: Key): BindingSet<E> {
        return sets.getOrPut(setKey) { BindingSet<Any?>(setKey) } as BindingSet<E>
    }

    fun getAll(): Map<Key, BindingSet<*>> = sets

}

class BindingSet<E> internal constructor(private val setKey: Key) {

    private val map = linkedMapOf<Key, Boolean>()

    fun addAll(other: BindingSet<E>) {
        other.map.forEach { (key, override) -> add(key, override) }
    }

    inline fun <reified T : E> add(
        elementName: Any? = null,
        override: Boolean = false
    ) {
        add<T>(typeOf(), elementName, override)
    }

    fun <T : E> add(
        elementType: Type<T>,
        elementName: Any? = null,
        override: Boolean = false
    ) {
        add(keyOf(elementType, elementName), override)
    }

    fun add(elementKey: Key, override: Boolean) {
        check(elementKey !in map || override) {
            "Already declared $elementKey in set $setKey"
        }

        map[elementKey] = override
    }

    fun getBindingSet(): Set<Key> = map.keys

}