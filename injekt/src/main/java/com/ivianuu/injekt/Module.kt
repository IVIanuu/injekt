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
 * A module is a collection of [Binding]s to drive [Component]s
 */
class Module @PublishedApi internal constructor() {

    internal val bindings = hashMapOf<Key, Binding<*>>()
    internal var mapBindings: MapBindings? = null
        private set
    internal var setBindings: SetBindings? = null
        private set

    fun <T> bind(binding: Binding<T>, key: Key, override: Boolean = false): BindingContext<T> {
        if (bindings.contains(key) && !override) {
            error("Already declared binding for $binding.key")
        }

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

    fun <K, V> bindIntoMap(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        entryValueType: Type<out V>,
        entryKey: K,
        mapName: Any? = null,
        entryValueName: Any? = null,
        override: Boolean = false
    ) {
        val mapKey = keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName)
        nonNullMapBindings().get<K, V>(mapKey)
            .put(entryKey, keyOf(entryValueType, entryValueName), override)
    }

    // todo rename
    fun <E> bindSet(
        setElementType: Type<E>,
        setName: Any? = null
    ) {
        val setKey = keyOf(typeOf<Any?>(Set::class, setElementType), setName)
        nonNullSetBindings().putIfAbsent(setKey)
    }

    fun <E> bindIntoSet(
        setElementType: Type<E>,
        elementType: Type<out E>,
        setName: Any? = null,
        elementName: Any? = null,
        override: Boolean = false
    ) {
        val setKey = keyOf(typeOf<Any?>(Set::class, setElementType), setName)
        nonNullSetBindings().get<E>(setKey)
            .put(keyOf(elementType, elementName), override)
    }

    private fun nonNullMapBindings(): MapBindings =
        mapBindings ?: MapBindings().also { mapBindings = it }

    private fun nonNullSetBindings(): SetBindings =
        setBindings ?: SetBindings().also { setBindings = it }


}

inline fun module(block: Module.() -> Unit): Module = Module().apply(block)

inline fun <reified T> Module.bind(
    binding: Binding<T>,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(binding, typeOf(), name, override)

fun <T> Module.bind(
    binding: Binding<T>,
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(binding, keyOf(type, name), override)

inline fun <reified T> Module.provide(
    name: Any? = null,
    scoped: Boolean = false,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = bind(typeOf(), name, scoped, override, definition)

fun <T> Module.bind(
    type: Type<T>,
    name: Any? = null,
    scoped: Boolean = false,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> {
    var binding = definitionBinding(definition)
    if (scoped) binding = binding.asScoped()
    return bind(binding, type, name, override)
}

inline fun <reified T> Module.bindWithState(
    name: Any? = null,
    scoped: Boolean = false,
    override: Boolean = false,
    noinline definition: StateDefinitionFactory.() -> StateDefinition<T>
): BindingContext<T> = bindWithState(typeOf(), name, scoped, override, definition)

fun <T> Module.bindWithState(
    type: Type<T>,
    name: Any? = null,
    scoped: Boolean = false,
    override: Boolean = false,
    definition: StateDefinitionFactory.() -> StateDefinition<T>
): BindingContext<T> {
    var binding = stateDefinitionBinding(definition)
    if (scoped) binding = binding.asScoped()
    return bind(binding, type, name, override)
}

inline fun <reified K, reified V> Module.bindMap(
    mapName: Any? = null
) {
    bindMap<K, V>(typeOf(), typeOf(), mapName)
}

inline fun <reified K, reified V, reified E : V> Module.bindIntoMap(
    entryKey: K,
    mapName: Any? = null,
    entryName: Any? = null,
    override: Boolean = false
) {
    bindIntoMap(
        typeOf(), typeOf<V>(),
        typeOf<E>(), entryKey, mapName, entryName, override
    )
}

inline fun <reified E> Module.bindSet(setName: Any? = null) {
    bindSet<E>(typeOf(), setName)
}

inline fun <reified E, reified V : E> Module.bindIntoSet(
    setName: Any? = null,
    elementName: Any? = null,
    override: Boolean = false
) {
    bindIntoSet(
        typeOf<E>(),
        typeOf<V>(), setName, elementName, override
    )
}