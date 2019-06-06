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
    private var mapBindings: MapBindings? = null
    private var setBindings: SetBindings? = null

    fun <T> bind(binding: Binding<T>, key: Key, override: Boolean = false): BindingContext<T> {
        if (bindings.contains(key) && !override) {
            error("Already declared binding for $binding.key")
        }

        binding.key = key
        binding.override = override

        bindings[key] = binding

        return BindingContext(binding, key, override, this)
    }

    fun include(module: Module) {
        module.bindings.forEach { bind(it.value, it.key, it.value.override) }
        module.mapBindings?.let { nonNullMapBindings().putAll(it) }
        module.setBindings?.let { nonNullSetBindings().putAll(it) }
    }

    // todo rename
    fun <K, V> bindMap(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        mapName: Any? = null
    ) {
        val mapKey = keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName)
        nonNullMapBindings().putIfAbsent(mapKey)
    }

    // todo rename
    fun <E> bindSet(
        setElementType: Type<E>,
        setName: Any? = null
    ) {
        val setKey = keyOf(typeOf<Any?>(Set::class, setElementType), setName)
        nonNullSetBindings().putIfAbsent(setKey)
    }

    fun <K, V> bindIntoMap(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        entryKey: K,
        entryValueType: Type<out V>,
        entryValueName: Any? = null,
        mapName: Any? = null,
        override: Boolean = false
    ) {
        val mapKey = keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName)
        nonNullMapBindings().get<K, V>(mapKey)
            .put(entryKey, keyOf(entryValueType, entryValueName), override)
    }

    fun <E> addBindingIntoSet(
        setElementType: Type<E>,
        elementKey: Key,
        elementBinding: Binding<out E>,
        setName: Any? = null,
        override: Boolean = false
    ) {
        val setKey = keyOf(typeOf<Any?>(Set::class, setElementType), setName)
        nonNullSetBindings().get<E>(setKey)
            .put(elementKey, elementBinding, override)
    }

    fun build(): Module = module(bindings, mapBindings, setBindings)

    private fun nonNullMapBindings(): MapBindings =
        mapBindings ?: MapBindings().also { mapBindings = it }

    private fun nonNullSetBindings(): SetBindings =
        setBindings ?: SetBindings().also { setBindings = it }

}

fun module(
    bindings: Map<Key, Binding<*>> = emptyMap(),
    mapBindings: MapBindings?,
    setBindings: SetBindings?
): Module = DefaultModule(bindings, mapBindings, setBindings)

inline fun module(block: ModuleBuilder.() -> Unit): Module = ModuleBuilder()
    .apply(block).build()

inline fun <reified T> ModuleBuilder.bind(
    binding: Binding<T>,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(binding, typeOf(), name, override)

fun <T> ModuleBuilder.bind(
    binding: Binding<T>,
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(binding, keyOf(type, name), override)

inline fun <reified K, reified V> ModuleBuilder.bindMap(
    mapName: Any? = null
) {
    bindMap<K, V>(typeOf(), typeOf(), mapName)
}

inline fun <reified K, reified V, reified E : V> ModuleBuilder.bindIntoMap(
    entryKey: K,
    mapName: Any? = null,
    entryName: Any? = null,
    override: Boolean = false
) {
    bindIntoMap(
        typeOf(), typeOf<V>(),
        entryKey, typeOf<E>(), entryName, mapName, override
    )
}

inline fun <reified E> ModuleBuilder.bindSet(setName: Any? = null) {
    bindSet<E>(typeOf(), setName)
}

inline fun <reified E> ModuleBuilder.addBindingIntoSet(
    elementKey: Key,
    elementBinding: Binding<out E>,
    setName: Any? = null,
    override: Boolean = false
) {
    addBindingIntoSet<E>(typeOf(), elementKey, elementBinding, setName, override)
}