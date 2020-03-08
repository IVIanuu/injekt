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
 * Result of call to [ComponentBuilder.bind]
 * this is allows to add additional aliases to the same declared binding
 *
 * E.g.
 *
 *```
 * factory { MyRepository() } // retrievable via component.get<MyRepository>()
 *     .bindAlias<IRepository>() // retrievable via component.get<IRepository>()
 *     .bindAlias(qualifier = "my_name")) // retrievable via component.get<MyRepository>(qualifier = qualifier("my_name")
 *```
 *
 * @see ComponentBuilder
 */
class BindingContext<T> internal constructor(
    val binding: Binding<T>,
    val componentBuilder: ComponentBuilder
) {

    inline fun <reified S> bindAlias(
        qualifier: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
    ): BindingContext<T> {
        bindAlias(key = keyOf<S>(qualifier = qualifier), duplicateStrategy = duplicateStrategy)
        return this
    }

    @JvmName("bindQualifiers")
    fun bindAlias(
        qualifier: Qualifier,
        duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
    ): BindingContext<T> {
        bindAlias(
            key = binding.key.copy(qualifier = qualifier),
            duplicateStrategy = duplicateStrategy
        )
        return this
    }

    /**
     * Creates an alias of the [binding] which can then also be retrieved via [key]
     *
     * For example the following code binds RepositoryImpl to Repository
     *
     * `factory { RepositoryImpl() }.bindAlias(keyOf<Repository>())`
     *
     * @see ComponentBuilder.bind
     */
    fun bindAlias(
        key: Key<*>,
        duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
    ): BindingContext<T> {
        componentBuilder.alias(
            originalKey = binding.key as Key<Any?>,
            aliasKey = key as Key<Any?>,
            duplicateStrategy = duplicateStrategy
        )

        return this
    }

    inline fun <reified K, reified V> intoMap(
        entryKey: K,
        mapQualifier: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
    ): BindingContext<T> = intoMap(
        entryKey = entryKey,
        mapKey = keyOf<Map<K, V>>(qualifier = mapQualifier),
        duplicateStrategy = duplicateStrategy
    )

    /**
     * Adds the [binding] into to the map of [mapKey]
     *
     * @see MultiBindingMap
     * @see MultiBindingMapBuilder
     */
    fun <K, V> intoMap(
        entryKey: K,
        mapKey: Key<Map<K, V>>,
        duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
    ): BindingContext<T> {
        componentBuilder.map(mapKey = mapKey) {
            put(
                entryKey = entryKey,
                entryValueKey = binding.key,
                duplicateStrategy = duplicateStrategy
            )
        }
        return this
    }

    inline fun <reified E> intoSet(
        setQualifier: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = intoSet(
        setKey = keyOf<Set<E>>(qualifier = setQualifier),
        duplicateStrategy = duplicateStrategy
    )

    /**
     * Adds the [binding] into to the set of [setKey]
     *
     * @see MultiBindingSet
     * @see MultiBindingSetBuilder
     */
    fun <E> intoSet(
        setKey: Key<Set<E>>,
        duplicateStrategy: DuplicateStrategy = binding.duplicateStrategy
    ): BindingContext<T> {
        componentBuilder.set(setKey = setKey) {
            add(
                elementKey = binding.key,
                duplicateStrategy = duplicateStrategy
            )
        }
        return this
    }
}
