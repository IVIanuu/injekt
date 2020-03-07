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
 * Result of call to [ComponentBuilder.add]
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
 * @see ComponentBuilder
 */
class BindingContext<T> internal constructor(
    val binding: Binding<T>,
    val componentBuilder: ComponentBuilder
) {

    inline fun <reified S> bindAlias(
        name: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        bindAlias(key = keyOf<S>(name = name), overrideStrategy = overrideStrategy)
        return this
    }

    @JvmName("bindName")
    fun bindAlias(
        name: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        bindAlias(key = binding.key.copy(name = name), overrideStrategy = overrideStrategy)
        return this
    }

    /**
     * Creates an alias of the [binding] which can then also be retrieved via [key]
     *
     * For example the following code binds RepositoryImpl to Repository
     *
     * `factory { RepositoryImpl() }.bindAlias(keyOf<Repository>())`
     *
     * @param key the alias key
     * @param overrideStrategy how overrides should be handled
     *
     * @see ComponentBuilder.add
     */
    fun bindAlias(
        key: Key<*>,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        componentBuilder.alias(
            originalKey = binding.key as Key<Any?>,
            aliasKey = key as Key<Any?>,
            overrideStrategy = overrideStrategy
        )

        return this
    }

    inline fun <reified K, reified V> intoMap(
        entryKey: K,
        mapName: Any? = null,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> = intoMap(
        entryKey = entryKey,
        mapKey = keyOf<Map<K, V>>(name = mapName),
        overrideStrategy = overrideStrategy
    )

    /**
     * Adds the [binding] into to the map with the [mapKey]
     *
     * @param entryKey the key of this binding in the map
     * @param mapKey the map this binding gets added to
     * @param overrideStrategy how overrides should be handled
     *
     * @see MultiBindingMap
     * @see MultiBindingMapBuilder
     */
    fun <K, V> intoMap(
        entryKey: K,
        mapKey: Key<Map<K, V>>,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        componentBuilder.map(mapKey = mapKey) {
            put(
                entryKey = entryKey,
                entryValueKey = binding.key,
                overrideStrategy = overrideStrategy
            )
        }
        return this
    }

    inline fun <reified E> intoSet(
        setName: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ): BindingContext<T> = intoSet(
        setKey = keyOf<Set<E>>(name = setName),
        overrideStrategy = overrideStrategy
    )

    /**
     * Adds the [binding] into to the set with the [setKey]
     *
     * @param setKey the set this binding gets added to
     * @param overrideStrategy how overrides should be handled
     *
     * @see MultiBindingSet
     * @see MultiBindingSetBuilder
     */
    fun <E> intoSet(
        setKey: Key<Set<E>>,
        overrideStrategy: OverrideStrategy = binding.overrideStrategy
    ): BindingContext<T> {
        componentBuilder.set(setKey = setKey) {
            add(
                elementKey = binding.key,
                overrideStrategy = overrideStrategy
            )
        }
        return this
    }
}
