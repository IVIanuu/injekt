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

    internal val bindings = mutableMapOf<Key, Binding<*>>()
    internal val includes = mutableSetOf<Module>()
    internal val mapBindings = mutableSetOf<Key>()
    internal val setBindings = mutableSetOf<Key>()

    fun bind(binding: Binding<*>) {
        if (bindings.put(binding.key, binding) != null && !binding.override) {
            error("Already declared binding for ${binding.key}")
        }
    }

    fun include(module: Module) {
        includes.add(module)
    }

    fun <K, V> bindMap(
        keyType: Type<K>,
        valueType: Type<V>,
        mapName: Qualifier? = null
    ) {
        mapBindings.add(
            Key(typeOf<Map<K, V>>(Map::class, keyType, valueType), mapName)
        )
    }

    fun <T> bindSet(
        elementType: Type<T>,
        setName: Qualifier? = null
    ) {
        setBindings.add(
            Key(typeOf<Set<T>>(Set::class, elementType), setName)
        )
    }

}

inline fun module(block: Module.() -> Unit): Module = Module().apply(block)

inline fun <reified T> Module.bind(
    kind: Kind,
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = bind(kind, typeOf(), name, override, definition)

fun <T> Module.bind(
    kind: Kind,
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    val binding = binding(kind, type, name, null, override, definition)
    bind(binding)
    return binding
}

inline fun <reified K, reified V> Module.bindMap(mapName: Qualifier? = null) {
    bindMap<K, V>(typeOf(), typeOf(), mapName)
}

inline fun <reified T> Module.bindSet(setName: Qualifier? = null) {
    bindSet<T>(typeOf(), setName)
}