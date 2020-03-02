/*
 * Copyright 2020 Manuel Wrage
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
 * Result of call to [ModuleBuilder.bind]
 * this is allows to add additional aliases to the same declared binding
 *
 * E.g.
 *
 *```
 * factory { MyRepository() } // retrievable via component.get<MyRepository>()
 *     .bindAlias<IRepository>() // retrievable via component.get<IRepository>()
 *     .bindAlias(name = "my_name") // retrievable via component.get<MyRepository>(name = "my_name")
 *```
 *
 * @see ModuleBuilder
 */
data class BindingContext<T> internal constructor(
    val binding: Binding<T>,
    val key: Key,
    val moduleBuilder: ModuleBuilder
) {

    inline fun <reified S> bindAlias(
        name: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        bindAlias(type = typeOf<S>(), name = name, overrideStrategy = overrideStrategy)
        return this
    }

    @JvmName("bindName")
    fun bindAlias(
        name: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        bindAlias(type = key.type, name = name, overrideStrategy = overrideStrategy)
        return this
    }

    /**
     * Binds the [binding] to [Module] with the alias params
     *
     * For example the following code binds RepositoryImpl to Repository
     *
     * `factory { RepositoryImpl() }.bindAlias(typeOf<Repository>())`
     *
     * @param type the alias type
     * @param name the alias name
     * @param override whether or not the alias binding can override existing bindings
     *
     * @see ModuleBuilder.bind
     */
    fun bindAlias(
        type: Type<*> = key.type,
        name: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        moduleBuilder.factory(
            type = type as Type<Any?>,
            name = name,
            overrideStrategy = overrideStrategy
        ) { parameters ->
            get(type = key.type as Type<Any?>, name = key.name, parameters = parameters)
        }

        return this
    }

    inline fun <reified K, reified V> intoMap(
        entryKey: K,
        mapName: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> = intoMap(
        mapKeyType = typeOf(),
        mapValueType = typeOf<V>(),
        entryKey = entryKey,
        mapName = mapName,
        overrideStrategy = overrideStrategy
    )

    /**
     * Contributes the [binding] into to the specified map
     *
     * @param mapKeyType the key type of the map
     * @param mapValueType the value type of the map
     * @param entryKey the key of this binding in the map
     * @param mapName the name of the map
     * @param override whether or not this binding can override existing one's
     *
     * @see MultiBindingMap
     * @see MultiBindingMapBuilder
     */
    fun <K, V> intoMap(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        entryKey: K,
        mapName: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        moduleBuilder.map(mapKeyType = mapKeyType, mapValueType = mapValueType, mapName = mapName) {
            put(
                entryKey = entryKey,
                entryValueType = key.type as Type<V>,
                entryValueName = key.name,
                overrideStrategy = overrideStrategy
            )
        }
        return this
    }

    inline fun <reified E> intoSet(
        setName: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ): BindingContext<T> = intoSet(
        setElementType = typeOf<E>(),
        setName = setName,
        overrideStrategy = overrideStrategy
    )

    /**
     * Contributes the [binding] into to the specified set
     *
     * @param setElementType the type of the set
     * @param setName the name of the set
     * @param override whether or not this binding can override existing one's
     *
     * @see MultiBindingSet
     * @see MultiBindingSetBuilder
     */
    fun <E> intoSet(
        setElementType: Type<E>,
        setName: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        moduleBuilder.set(setElementType = setElementType, setName = setName) {
            add(
                elementType = key.type as Type<E>,
                elementName = key.name,
                overrideStrategy = overrideStrategy
            )
        }
        return this
    }
}
