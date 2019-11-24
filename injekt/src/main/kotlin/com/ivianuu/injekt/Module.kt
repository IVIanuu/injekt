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

/**
 * Construct a [Module] with a lambda
 *
 * @param block the block to configure the module
 * @return the constructed module
 *
 * @see Module
 */
fun module(block: Module.() -> Unit): Module = Module().apply(block)

/**
 * A module is a collection of [Binding]s to drive [Component]s
 *
 * A typical module might look like this:
 *
 * ´´´
 * val myRepositoryModule = module {
 *     single { MyRepository(api = get(), database = get()) }
 *     single { MyApi(serializer = get()) }
 *     single { MyDatabase(databaseFile = get()) }
 * }
 * ´´´
 *
 * @see ComponentBuilder.modules
 */
class Module internal constructor() {

    internal val bindings = mutableMapOf<Key, Binding<*>>()
    internal val mapBindings = MapBindings()
    internal val setBindings = SetBindings()

    inline fun <reified T> factory(
        name: Any? = null,
        override: Boolean = false,
        noinline definition: Definition<T>
    ): BindingContext<T> = factory(
        type = typeOf(),
        name = name,
        override = override,
        definition = definition
    )

    /**
     * Contributes a binding which will be instantiated on each request
     *
     * @param type the of the instance
     * @param name the name of the instance
     * @param override whether or not the binding can override existing bindings
     * @param definition the definitions which creates instances
     *
     * @see bind
     */
    fun <T> factory(
        type: Type<T>,
        name: Any? = null,
        override: Boolean = false,
        definition: Definition<T>
    ): BindingContext<T> = bind(
        key = keyOf(type, name),
        binding = definitionBinding(optimizing = true, definition = definition),
        override = override,
        scoped = false
    )

    inline fun <reified T> single(
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

    /**
     * Contributes a binding which will be reused throughout the lifetime of the component it life's in
     *
     * @param type the of the instance
     * @param name the name of the instance
     * @param override whether or not the binding can override existing bindings
     * @param eager whether the instance should be created when the component get's created
     * @param definition the definitions which creates instances
     *
     * @see bind
     */
    fun <T> single(
        type: Type<T>,
        name: Any? = null,
        override: Boolean = false,
        eager: Boolean = false,
        definition: Definition<T>
    ): BindingContext<T> =
        bind(
            key = keyOf(type, name),
            binding = definitionBinding(optimizing = false, definition = definition).asSingle(),
            override = override,
            eager = eager,
            scoped = true
        )

    /**
     * Adds a binding for a already existing instance
     *
     * @param instance the instance to contribute
     * @param type the type for the [Key] by which the binding can be retrieved later in the component
     * @param name the type for the [Key] by which the binding can be retrieved later in the component
     * @return the [BindingContext] to chain binding calls
     *
     * @see bind
     */
    fun <T> instance(
        instance: T,
        type: Type<T> = typeOf(instance),
        name: Any? = null,
        override: Boolean = false
    ): BindingContext<T> = bind(
        key = keyOf(type, name),
        binding = LinkedInstanceBinding(instance),
        override = override,
        scoped = false
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
     * ´@Inject class MyRepository : Repository`
     *
     * ´´´
     * withBinding(type = typeOf<MyRepository>()) {
     *     bindType<Repository>()
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
        bindProxy(type = type, name = name).block()
    }

    /**
     * Merges the contents of the other module into this one
     *
     * @param module the module to merge
     */
    fun include(module: Module) {
        module.bindings.forEach {
            bind(
                key = it.key,
                binding = it.value,
                override = it.value.override,
                eager = it.value.eager,
                scoped = it.value.scoped
            )
        }
        mapBindings.putAll(module.mapBindings)
        setBindings.addAll(module.setBindings)
    }

    inline fun <reified K, reified V> map(
        mapName: Any? = null,
        noinline block: BindingMap<K, V>.() -> Unit = {}
    ) {
        map(mapKeyType = typeOf(), mapValueType = typeOf(), mapName = mapName, block = block)
    }

    /**
     * Runs a lambda in the scope of a contributed binding map
     * Creates and adds a new binding set if it does not exist yet in this module
     *
     * @param mapKeyType the type of the keys in the map
     * @param mapValueType the type of the values in the map
     * @param mapName the name by which the map can be retrieved later in the component
     * @param block the lambda to run in the context of the binding map
     *
     * @see BindingMap
     */
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

    inline fun <reified E> set(
        setName: Any? = null,
        noinline block: BindingSet<E>.() -> Unit = {}
    ) {
        set(setElementType = typeOf(), setName = setName, block = block)
    }

    /**
     * Runs a lambda in the scope of a contributed binding set
     * Creates and adds a new binding set if it does not exist yet in this module
     *
     * @param setElementType the type of the elements in the set
     * @param setName the name by which the set can be retrieved later in the component
     * @param block the lambda to run in the context of the binding set
     *
     * @see BindingSet
     */
    fun <E> set(
        setElementType: Type<E>,
        setName: Any? = null,
        block: BindingSet<E>.() -> Unit = {}
    ) {
        val setKey = keyOf(type = typeOf<Any?>(Set::class, setElementType), name = setName)
        setBindings.get<E>(setKey).apply(block)
    }

    /**
     * Contributes the binding
     * This function is rarely used directly instead use [factory] or [single]
     *
     * @param key the key by which the binding can be retrieved later in the component
     * @param binding the binding to add
     * @param override whether or not the binding can override existing bindings
     * @param eager whether a instance should be created when the component get's created
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
        binding: Binding<T>,
        override: Boolean = false,
        eager: Boolean = false,
        scoped: Boolean = false
    ): BindingContext<T> {
        check(key !in bindings || override) {
            "Already declared binding for $binding.key"
        }

        binding.override = override
        binding.eager = eager
        binding.scoped = scoped

        bindings[key] = binding

        return BindingContext(binding = binding, key = key, module = this)
    }

    private fun <T> bindProxy(
        type: Type<T>,
        name: Any?
    ): BindingContext<T> {
        // we create a proxy binding which links to the original binding
        // because we have no reference to the original one it's likely in another module or component
        // we use a unique id here to make sure that the binding does not collide with any user config
        return bind(
            key = keyOf(type = type, name = UUID.randomUUID().toString()),
            binding = UnlinkedProxyBinding(originalKey = keyOf(type = type, name = name))
        )
    }

}