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

import java.util.*

fun module(block: Module.() -> Unit): Module = Module().apply(block)

inline fun <reified T> Module.factory(
    name: Any? = null,
    override: Boolean = false,
    optimizing: Boolean = true,
    noinline definition: Definition<T>
): BindingContext<T> = factory(
    type = typeOf(),
    name = name,
    override = override,
    optimizing = optimizing,
    definition = definition
)

fun <T> Module.factory(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    optimizing: Boolean = true,
    definition: Definition<T>
): BindingContext<T> = bind(
    key = keyOf(type, name),
    binding = definitionBinding(optimizing = optimizing, definition = definition),
    override = override,
    unscoped = true
)

inline fun <reified T> Module.single(
    name: Any? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = single(
    type = typeOf(),
    name = name,
    override = override,
    eager = eager,
    definition = definition
)

fun <T> Module.single(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): BindingContext<T> =
    bind(
        key = keyOf(type, name),
        binding = definitionBinding(optimizing = false, definition = definition).asScoped(),
        override = override,
        eager = eager,
        unscoped = false
    )

inline fun <reified K, reified V> Module.map(
    mapName: Any? = null,
    noinline block: BindingMap<K, V>.() -> Unit = {}
) {
    map(mapKeyType = typeOf(), mapValueType = typeOf(), mapName = mapName, block = block)
}

inline fun <reified E> Module.set(
    setName: Any? = null,
    noinline block: BindingSet<E>.() -> Unit = {}
) {
    set(setElementType = typeOf(), setName = setName, block = block)
}

inline fun <reified T> Module.withBinding(
    name: Any? = null,
    noinline block: BindingContext<T>.() -> Unit
) {
    withBinding(type = typeOf(), name = name, block = block)
}

fun <T> Module.withBinding(
    type: Type<T>,
    name: Any? = null,
    block: BindingContext<T>.() -> Unit
) {
    bindProxy(type = type, name = name).block()
}

private fun <T> Module.bindProxy(
    type: Type<T>,
    name: Any?
): BindingContext<T> {
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // this binding acts as proxy and just calls trough the original implementation and should be never injected directly
    return bind(
        key = keyOf(type = type, name = UUID.randomUUID().toString()),
        binding = UnlinkedProxyBinding(originalKey = keyOf(type = type, name = name))
    )
}

inline fun <reified T> Module.instance(
    instance: T,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = instance(
    instance = instance,
    type = typeOf(),
    name = name,
    override = override
)

fun <T> Module.instance(
    instance: T,
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(
    key = keyOf(type, name),
    binding = LinkedInstanceBinding(instance),
    override = override,
    unscoped = false
)

/**
 * A module is a collection of [Binding]s to drive [Component]s
 */
class Module internal constructor() {

    internal val bindings = mutableMapOf<Key, Binding<*>>()
    internal val mapBindings = MapBindings()
    internal val setBindings = SetBindings()

    fun <T> bind(
        key: Key,
        binding: Binding<T>,
        override: Boolean = false,
        eager: Boolean = false,
        unscoped: Boolean = true
    ): BindingContext<T> {
        check(key !in bindings || override) {
            "Already declared binding for $binding.key"
        }

        binding.override = override
        binding.eager = eager
        binding.unscoped = unscoped

        bindings[key] = binding

        return BindingContext(binding, key, override, this)
    }

    fun include(module: Module) {
        module.bindings.forEach {
            bind(
                key = it.key,
                binding = it.value,
                override = it.value.override,
                unscoped = it.value.unscoped
            )
        }
        mapBindings.putAll(module.mapBindings)
        setBindings.addAll(module.setBindings)
    }

    fun <K, V> map(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        mapName: Any? = null,
        block: BindingMap<K, V>.() -> Unit = {}
    ) {
        val mapKey = keyOf(
            type = typeOf<Any?>(Map::class, mapKeyType, mapValueType),
            name = mapName
        )
        mapBindings.get<K, V>(mapKey).apply(block)
    }

    fun <E> set(
        setElementType: Type<E>,
        setName: Any? = null,
        block: BindingSet<E>.() -> Unit = {}
    ) {
        val setKey = keyOf(type = typeOf<Any?>(Set::class, setElementType), name = setName)
        setBindings.get<E>(setKey).apply(block)
    }

}