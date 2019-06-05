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

class ModuleBuilder {

    private val bindings = mutableMapOf<Key, Binding<*>>()
    private val mapBindings = mutableMapOf<Key, MutableMap<Any?, Binding<*>>>()
    private val setBindings = mutableMapOf<Key, MutableSet<Binding<*>>>()

    fun <T> add(
        binding: Binding<T>,
        key: Key,
        override: Boolean = false
    ): BindingContext<T> {
        if (bindings.put(key, binding) != null && !override) {
            error("Already declared binding for $key")
        }

        return BindingContext(binding, key, override, this)
    }

    fun include(module: Module) {
        module.bindings.forEach { add(it.value, it.key) }
        module.mapBindings.forEach { (mapKey, map) ->
            map.forEach { (entryKey, entryValueBinding) ->
                addBindingIntoMap(mapKey, entryKey, entryValueBinding)
            }
        }
        module.setBindings.forEach { (setKey, set) ->
            set.forEach { elementBinding ->
                addBindingIntoSet(setKey, elementBinding)
            }
        }
    }

    // todo rename
    fun mapBinding(mapKey: Key) {
        mapBindings.getOrPut(mapKey) { mutableMapOf() }
    }

    // todo rename
    fun setBinding(setKey: Key) {
        setBindings.getOrPut(setKey) { mutableSetOf() }
    }

    fun addBindingIntoMap(
        mapKey: Key,
        entryKey: Any?,
        entryValueBinding: Binding<*>,
        override: Boolean = false
    ) {
        val map = mapBindings.getOrPut(mapKey) { mutableMapOf() }
        val oldEntryValueBinding = map[entryKey]
        if (oldEntryValueBinding != null && !override) {
            error("Already added value for $entryKey in map $mapKey")
        }

        map[entryKey] = entryValueBinding
    }

    fun addBindingIntoSet(
        setKey: Key,
        elementBinding: Binding<*>
    ) {
        val set = setBindings.getOrPut(setKey) { mutableSetOf() }
        if (!set.add(elementBinding)) {
            error("Already added $elementBinding to set $setKey")
        }
    }

    fun build(): Module = module(bindings, mapBindings, setBindings)
}

fun module(
    bindings: Map<Key, Binding<*>> = emptyMap(),
    mapBindings: Map<Key, Map<Any?, Binding<*>>>,
    setBindings: Map<Key, Set<Binding<*>>>
): Module = SimpleModule(bindings, mapBindings, setBindings)

inline fun module(block: ModuleBuilder.() -> Unit): Module = ModuleBuilder()
    .apply(block).build()

inline fun <reified T> ModuleBuilder.add(
    binding: Binding<T>,
    name: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = add(binding, typeOf(), name, override)

fun <T> ModuleBuilder.add(
    binding: Binding<T>,
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = add(binding, keyOf(type, name), override)

inline fun <reified K, reified V> ModuleBuilder.mapBinding(
    mapName: Qualifier? = null
) {
    mapBinding<K, V>(typeOf(), typeOf(), mapName)
}

fun <K, V> ModuleBuilder.mapBinding(
    mapKeyType: Type<K>,
    mapValueType: Type<V>,
    mapName: Qualifier? = null
) {
    mapBinding(keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName))
}

inline fun <reified T> ModuleBuilder.setBinding(setName: Qualifier? = null) {
    setBinding<T>(typeOf(), setName)
}

fun <T> ModuleBuilder.setBinding(
    setType: Type<T>,
    setName: Qualifier? = null
) {
    setBinding(keyOf(typeOf<Any?>(Set::class, setType), setName))
}