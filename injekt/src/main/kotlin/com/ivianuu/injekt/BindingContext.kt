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

/**
 * Result of call to [Module.bind]
 * This is allows to add additional aliases to the same declared binding
 *
 * E.g.
 *
 *```
 * factory { MyRepository() } // retrievable via component.get<MyRepository>()
 *     .bindType<IRepository>() // retrievable via component.get<IRepository>()
 *     .bindName("my_name") // retrievable via component.get<MyRepository>(name = "my_name")
 *```
 *
 */
data class BindingContext<T> internal constructor(
    val binding: Binding<T>,
    val key: Key,
    val module: Module
) {

    inline fun <reified S> bindAlias(
        name: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> {
        bindAlias(typeOf<S>(), name, override)
        return this
    }

    /**
     * Binds the [binding] to [module] with the alias params
     *
     * For example the following code binds RepositoryImpl to Repository
     *
     * `factory { RepositoryImpl() }.bindAlias(typeOf<Repository>())`
     *
     * @param type the alias type
     * @param name the alias name
     * @param override whether or not the alias binding can override existing bindings
     *
     * @see Module.bind
     */
    fun bindAlias(
        type: Type<*>,
        name: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> {
        module.bind(
            key = keyOf(type, name),
            binding = binding as Binding<Any?>,
            override = override,
            eager = binding.eager,
            unscoped = binding.unscoped
        )

        return this
    }

    inline fun <reified T> bindType() {
        bindAlias(typeOf<T>())
    }

    /**
     * @see bindAlias
     */
    fun bindType(type: Type<*>): BindingContext<T> {
        bindAlias(type)
        return this
    }

    /**
     * @see bindAlias
     */
    fun bindName(name: Any): BindingContext<T> {
        bindAlias(key.type, name)
        return this
    }

    inline fun <reified K, reified V> intoMap(
        entryKey: K,
        mapName: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> = intoMap(typeOf<K>(), typeOf<V>(), entryKey, mapName, override)

    /**
     * Contributes the [binding] into to the specified map
     *
     * @param mapKeyType the key type of the map
     * @param mapValueType the value type of the map
     * @param entryKey the key of this binding in the map
     * @param mapName the name of the map
     * @param override whether or not this binding can override existing one's
     *
     * @see BindingMap
     */
    fun <K, V> intoMap(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        entryKey: K,
        mapName: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> {
        module.map(mapKeyType, mapValueType, mapName) {
            put(entryKey, key.type as Type<V>, key.name, override)
        }
        return this
    }

    inline fun <reified E> intoSet(
        setName: Any? = null,
        override: Boolean = false
    ): BindingContext<T> = intoSet(typeOf<E>(), setName, override)

    /**
     * Contributes the [binding] into to the specified set
     *
     * @param setElementType the type of the set
     * @param setName the name of the set
     * @param override whether or not this binding can override existing one's
     *
     * @see BindingSet
     */
    fun <E> intoSet(
        setElementType: Type<E>,
        setName: Any? = null,
        override: Boolean = binding.override
    ): BindingContext<T> {
        module.set(setElementType, setName) {
            add(key.type as Type<E>, key.name, override)
        }
        return this
    }

}