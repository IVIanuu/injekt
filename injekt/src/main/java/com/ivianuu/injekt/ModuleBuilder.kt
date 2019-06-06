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

    private val bindings = mutableMapOf<Key, BindingContribution<*>>()
    private val mapBindings = MapBindings() // todo maybe lazy create
    private val setBindings = mutableMapOf<Key, MutableSet<SetContribution<*>>>()

    fun <T> bind(binding: Binding<T>, key: Key, override: Boolean = false): BindingContext<T> {
        if (bindings.contains(key) && !override) {
            error("Already declared binding for $binding.key")
        }

        bindings[key] = BindingContribution(binding, key, override)

        return BindingContext(binding, key, override, this)
    }

    fun include(module: Module) {
        module.bindings.forEach { bind(it.value.binding, it.value.key, it.value.override) }
        module.mapBindings?.let { mapBindings.putAll(it) }
        module.setBindings.forEach { (setKey, set) ->
            set.forEach { contribution ->
                addBindingIntoSet(setKey, contribution.binding, contribution.override)
            }
        }
    }

    // todo rename
    fun bindMap(mapKey: Key) {
        mapBindings.putIfAbsent(mapKey)
    }

    // todo rename
    fun bindSet(setKey: Key) {
        setBindings.getOrPut(setKey) { mutableSetOf() }
    }

    fun addBindingIntoMap(
        mapKey: Key,
        entryKey: Any?,
        entryValueBinding: Binding<*>,
        override: Boolean = false
    ) {
        mapBindings.get<Any?, Any?>(mapKey)
            .put(entryKey, entryValueBinding as Binding<Any?>, override)
    }

    fun addBindingIntoSet(
        setKey: Key,
        elementBinding: Binding<*>,
        override: Boolean = false
    ) {
        val set = setBindings.getOrPut(setKey) { mutableSetOf() }
        if (set.any { it.binding == elementBinding } && !override) {
            error("Already added $elementBinding to set $setKey")
        }

        set.removeAll { it.binding == elementBinding }
        set.add(SetContribution(elementBinding, override))
    }

    fun build(): Module = module(bindings, mapBindings, setBindings)
}

fun module(
    bindings: Map<Key, BindingContribution<*>> = emptyMap(),
    mapBindings: MapBindings?,
    setBindings: Map<Key, Set<SetContribution<*>>>
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

fun <K, V> ModuleBuilder.bindMap(
    mapKeyType: Type<K>,
    mapValueType: Type<V>,
    mapName: Any? = null
) {
    bindMap(keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName))
}

inline fun <reified E> ModuleBuilder.bindSet(setName: Any? = null) {
    bindSet<E>(typeOf(), setName)
}

fun <E> ModuleBuilder.bindSet(
    setElementType: Type<E>,
    setName: Any? = null
) {
    bindSet(keyOf(typeOf<Any?>(Set::class, setElementType), setName))
}

inline fun <reified K, reified V> ModuleBuilder.addBindingIntoMap(
    entryKey: K,
    entryValueBinding: Binding<out V>,
    mapName: Any? = null,
    override: Boolean = false
) {
    addBindingIntoMap(typeOf(), typeOf(), entryKey, entryValueBinding, mapName, override)
}

fun <K, V> ModuleBuilder.addBindingIntoMap(
    mapKeyType: Type<K>,
    mapValueType: Type<V>,
    entryKey: K,
    entryValueBinding: Binding<out V>,
    mapName: Any? = null,
    override: Boolean = false
) {
    val mapKey = keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName)
    addBindingIntoMap(mapKey, entryKey, entryValueBinding, override)
}

inline fun <reified E> ModuleBuilder.addBindingIntoSet(
    elementBinding: Binding<out E>,
    setName: Any? = null,
    override: Boolean = false
) {
    addBindingIntoSet<E>(typeOf(), elementBinding, setName, override)
}

fun <E> ModuleBuilder.addBindingIntoSet(
    setElementType: Type<E>,
    elementBinding: Binding<out E>,
    setName: Any? = null,
    override: Boolean = false
) {
    val setKey = keyOf(typeOf<Any?>(Set::class, setElementType), setName)
    addBindingIntoSet(setKey, elementBinding, override)
}