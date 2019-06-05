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
interface Module {
    val bindings: Map<Key, Binding<*>>
}

class SimpleModule(override val bindings: Map<Key, Binding<*>>) : Module

class ModuleBuilder {
    private val bindings = mutableMapOf<Key, Binding<*>>()

    fun <T> add(
        binding: Binding<T>,
        key: Key,
        override: Boolean = false
    ): Binding<T> {
        if (bindings.put(key, binding) != null && !override) {
            error("Already declared binding for $key")
        }

        return binding
    }

    fun include(module: Module) {
        module.bindings.forEach { add(it.value, it.key) }
    }

    /*

   fun <T> Binding<T>.bind(key: Key, override: Boolean = false): Binding<T> =
            add(this, key, override)

    fun <T> Binding<T>.bind(type: Type<*>, name: Qualifier? = null): Binding<T> =
        bind(keyOf(type, name))

    inline fun <reified T> Binding<*>.bindType() {
        bind(typeOf<T>())
    }

    infix fun <T> Binding<T>.bindType(type: Type<*>): Binding<T> =
        bind(type)

    fun <T> Binding<T>.bindTypes(vararg types: Type<*>): Binding<T> {
        types.forEach { bindType(it) }
        return this
    }

    infix fun <T> Binding<T>.bindTypes(types: Iterable<KClass<*>>): Binding<T> {
        types.forEach { bindTypes(it) }
        return this
    }

    infix fun <T> Binding<T>.bindName(name: Qualifier): Binding<T> =
        additionalKey(Key(type, name))

    fun <T> Binding<T>.bindNames(vararg names: Qualifier): Binding<T> {
        names.forEach { bindName(it) }
        return this
    }

    infix fun <T> Binding<T>.bindNames(names: Iterable<Qualifier>): Binding<T> {
        names.forEach { bindName(it) }
        return this
    }

    inline fun <reified T> Binding<*>.bindAlias(name: Qualifier) {
        bindAlias(typeOf<T>(), name)
    }

    fun <T> Binding<T>.bindAlias(type: Type<*>, name: Qualifier): Binding<T> =
        additionalKey(Key(type, name))

    infix fun <T> Binding<T>.bindAlias(pair: Pair<Type<*>, Qualifier>): Binding<T> {
        bindAlias(pair.first, pair.second)
        return this
    }

    infix fun <T : V, K, V> Binding<T>.bindIntoMap(mapBinding: MapBinding<K, V>): Binding<T> {
        mapBindings[mapBinding.mapKey] = mapBinding
        return this
    }

    inline fun <reified T : V, reified K, reified V> Binding<T>.bindIntoMap(
        key: K,
        keyType: Type<K> = typeOf(),
        valueType: Type<V> = typeOf(),
        mapName: Qualifier? = null,
        override: Boolean = false
    ): Binding<T> {
        bindIntoMap(mapBinding(key, keyType, valueType, mapName, override))
        return this
    }

    infix fun <T : V, V> Binding<T>.bindIntoSet(setBinding: SetBinding<V>): Binding<T> {
        setBindings[setBinding.setKey] = setBinding
        return this
    }

    inline fun <reified T : V, reified V> Binding<T>.bindIntoSet(
        setType: Type<T> = typeOf(),
        setName: Qualifier? = null,
        override: Boolean = false
    ): Binding<T> {
        bindIntoSet(setBinding(setType, setName, override))
        return this
    }*/

    fun build(): Module = SimpleModule(bindings)
}

inline fun module(block: ModuleBuilder.() -> Unit): Module = ModuleBuilder()
    .apply(block).build()

inline fun <reified T> ModuleBuilder.add(
    binding: Binding<T>,
    name: Qualifier? = null,
    override: Boolean = false
): Binding<T> = add(binding, typeOf(), name, override)

fun <T> ModuleBuilder.add(
    binding: Binding<T>,
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false
): Binding<T> = add(binding, keyOf(type, name), override)