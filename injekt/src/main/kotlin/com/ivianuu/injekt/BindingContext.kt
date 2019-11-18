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

// todo clean multi binding api mess

/**
 * Result of call to [Module.bind]
 * This is allows to add additional aliases to the declared [binding]
 *
 * E.g.
 *
 * factory { MyRepository() } bindType type<IRepository>()
 *
 */
data class BindingContext<T> internal constructor(
    /**
     * The binding added in the [Module.bind] call
     */
    val binding: Binding<T>,

    /**
     * The binding added in the [Module.bind] call
     */
    val key: Key,

    /**
     * The module the [binding] was added to
     */
    val module: Module
) {

    inline fun <reified T> bindAlias(
        name: Any? = null,
        override: Boolean = binding.override
    ) {
        bindAlias(typeOf<T>(), name, override)
    }

    fun bindAlias(
        type: Type<*>,
        name: Any? = null,
        override: Boolean = binding.override
    ) {
        module.bind(
            key = keyOf(type, name),
            binding = binding as Binding<Any?>,
            override = override,
            eager = binding.eager,
            unscoped = binding.unscoped
        )
    }

    inline fun <reified T> bindType() {
        bindAlias(typeOf<T>())
    }

    infix fun bindType(type: Type<*>): BindingContext<T> {
        bindAlias(type)
        return this
    }

    infix fun bindName(name: Any): BindingContext<T> {
        bindAlias(key.type, name)
        return this
    }

    inline fun <reified T : V, reified K, reified V> intoMap(
        entryKey: K,
        mapName: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> = intoMap(typeOf(), typeOf<V>(), entryKey, mapName, override)

    fun <T : V, K, V> intoMap(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        entryKey: K,
        mapName: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> {
        module.map(mapKeyType, mapValueType, mapName) {
            put(entryKey, key.type as Type<T>, key.name, override)
        }
        return this as BindingContext<T> // todo
    }

    inline fun <reified T : E, reified E> intoSet(
        setName: Any? = null,
        override: Boolean = false
    ): BindingContext<T> = intoSet(typeOf<E>(), setName, override)

    /**
     * Contributes the [binding] into to the specified set
     *
     * @param
     */
    fun <T : E, E> intoSet(
        setElementType: Type<E>,
        setName: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> {
        module.set(setElementType, setName) {
            add(key.type as Type<T>, key.name, override)
        }
        return this as BindingContext<T> // todo
    }

}