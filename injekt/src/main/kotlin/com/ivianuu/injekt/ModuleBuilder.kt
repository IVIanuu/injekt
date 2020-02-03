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

import java.util.UUID

/**
 * Construct a [Module] with a lambda
 *
 * @param block the block to configure the Module
 * @return the constructed Module
 *
 * @see Module
 * @see ModuleBuilder
 */
inline fun Module(block: ModuleBuilder.() -> Unit): Module = ModuleBuilder().apply(block).build()

/**
 * Builder for a [Module]
 *
 * @see Module
 */
class ModuleBuilder {

    private val bindings = mutableMapOf<Key, Binding<*>>()
    private val multiBindingMapBuilders = mutableMapOf<Key, MultiBindingMapBuilder<Any?, Any?>>()
    private val multiBindingSetBuilders = mutableMapOf<Key, MultiBindingSetBuilder<Any?>>()

    inline fun <reified T> instance(
        instance: T,
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ) {
        instance(
            instance = instance,
            type = typeOf(),
            name = name,
            overrideStrategy = overrideStrategy
        )
    }

    /**
     * Adds a binding for a already existing instance
     *
     * @param instance the instance to contribute
     * @param type the type for the [Key] by which the binding can be retrieved later in the [Component]
     * @param name the type for the [Key] by which the binding can be retrieved later in the [Component]
     * @return the [BindingContext] to chain binding calls
     *
     * @see bind
     */
    fun <T> instance(
        instance: T,
        type: Type<T>,
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ): BindingContext<T> = bind(
        key = keyOf(type, name),
        binding = InstanceBinding(
            instance = instance,
            overrideStrategy = overrideStrategy,
            scoped = false
        )
    )

    inline fun <reified T> withBinding(
        name: Any? = null,
        noinline block: BindingContext<T>.() -> Unit
    ) {
        withBinding(type = typeOf(), name = name, block = block)
    }

    /**
     * Invokes a lambda in the binding context of a other binding
     * This allows to add aliases to bindings which are declared somewhere else
     *
     * For example to add a alias for a annotated class one can write the following:
     *
     * ´@Factory class MyRepository : Repository`
     *
     * ´´´
     * withBinding(type = typeOf<MyRepository>()) {
     *     bindAlias<Repository>()
     * }
     *
     * ´´´
     *
     * @param type the type of the binding
     * @param name the name of the other binding
     * @param block the lambda to call in the context of the other binding
     */
    fun <T> withBinding(
        type: Type<T>,
        name: Any? = null,
        block: BindingContext<T>.() -> Unit
    ) {
        // we create a proxy binding which links to the original binding
        // because we have no reference to the original one it's likely in another [Module] or [Component]
        // we use a unique id here to make sure that the binding does not collide with any user config
        factory(type = type, name = UUID.randomUUID().toString()) { get(type = type, name = name) }
            .block()
    }

    /**
     * Merges the contents of the other Module into this one
     *
     * @param module the Module to merge
     */
    fun include(module: Module) {
        module.bindings.forEach {
            bind(key = it.key, binding = it.value)
        }

        module.multiBindingMaps.forEach { (mapKey, map) ->
            val builder = multiBindingMapBuilders.getOrPut(mapKey) {
                MultiBindingMapBuilder(mapKey)
            }
            map.entries.forEach { (entryKey, entry) ->
                builder.put(entryKey, entry)
            }
        }

        module.multiBindingSets.forEach { (setKey, set) ->
            val builder = multiBindingSetBuilders.getOrPut(setKey) {
                MultiBindingSetBuilder(setKey)
            }
            set.forEach { element -> builder.add(element) }
        }
    }

    inline fun <reified K, reified V> map(
        mapName: Any? = null,
        noinline block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
    ) {
        map(mapKeyType = typeOf(), mapValueType = typeOf(), mapName = mapName, block = block)
    }

    /**
     * Runs a lambda in the scope of a [MultiBindingMapBuilder]
     *
     * @param mapKeyType the type of the keys in the map
     * @param mapValueType the type of the values in the map
     * @param mapName the name by which the map can be retrieved later in the [Component]
     * @param block the lambda to run in the context of the binding map
     *
     * @see MultiBindingMap
     */
    fun <K, V> map(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        mapName: Any? = null,
        block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
    ) {
        val mapKey = keyOf(
            type = typeOf<Any?>(Map::class, arrayOf(mapKeyType, mapValueType)),
            name = mapName
        )

        map(mapKey = mapKey, block = block)
    }

    /**
     * Runs a lambda in the scope of a [MultiBindingMapBuilder]
     *
     * @param mapKey the key of the map
     * @param block the lambda to run in the context of the binding map
     *
     * @see MultiBindingMap
     */
    fun <K, V> map(
        mapKey: Key,
        block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
    ) {
        val builder = multiBindingMapBuilders.getOrPut(mapKey) {
            MultiBindingMapBuilder(mapKey)
        } as MultiBindingMapBuilder<K, V>

        builder.apply(block)
    }

    inline fun <reified E> set(
        setName: Any? = null,
        noinline block: MultiBindingSetBuilder<E>.() -> Unit = {}
    ) {
        set(setElementType = typeOf(), setName = setName, block = block)
    }

    fun <E> set(
        setElementType: Type<E>,
        setName: Any? = null,
        block: MultiBindingSetBuilder<E>.() -> Unit = {}
    ) {
        val setKey = keyOf(type = typeOf<Any?>(Set::class, arrayOf(setElementType)), name = setName)
        set(setKey = setKey, block = block)
    }

    /**
     * Runs a lambda in the scope of a [MultiBindingSetBuilder]
     *
     * @param setKey the key of the set
     * @param block the lambda to run in the context of the binding set
     *
     * @see MultiBindingSet
     */
    fun <E> set(
        setKey: Key,
        block: MultiBindingSetBuilder<E>.() -> Unit = {}
    ) {
        val builder = multiBindingSetBuilders.getOrPut(setKey) {
            MultiBindingSetBuilder(setKey)
        } as MultiBindingSetBuilder<E>

        builder.apply(block)
    }

    /**
     * Contributes the binding
     * This function is rarely used directly instead use [factory] or [single]
     *
     * @param key the key by which the binding can be retrieved later in the [Component]
     * @param binding the binding to add
     * @param overrideStrategy the strategy for handling overrides
     * @param eager whether a instance should be created when the [Component] get's created
     * @param scoped whether instances should be created in the requesting scope
     * @return the [BindingContext] to chain binding calls
     *
     * @see Component.get
     * @see BindingContext
     * @see factory
     * @see single
     */
    fun <T> bind(
        key: Key,
        binding: Binding<T>
    ): BindingContext<T> {
        if (binding.overrideStrategy.check(
                existsPredicate = { key in bindings },
                errorMessage = { "Already declared binding for $key" }
            )
        ) {
            bindings[key] = binding
        }

        return BindingContext(binding = binding, key = key, moduleBuilder = this)
    }

    /**
     * Create a new [Module] instance.
     */
    fun build(): Module {
        return Module(
            bindings = bindings,
            multiBindingMaps = multiBindingMapBuilders.mapValues { it.value.build() },
            multiBindingSets = multiBindingSetBuilders.mapValues { it.value.build() }
        )
    }

}

internal class InstanceBinding<T>(
    val instance: T,
    eager: Boolean = false,
    scoped: Boolean = false,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
) : Binding<T>(FactoryKind, eager, scoped, overrideStrategy) {
    override fun link(component: Component): Provider<T> = InstanceProvider(instance)

    class InstanceProvider<T>(private val instance: T) : Provider<T> {
        override fun invoke(parameters: Parameters): T = instance
    }
}
