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
 * Construct a [Component] with a lambda
 *
 * @param block the block to configure the Component
 * @return the constructed [Component]
 *
 * @see Component
 */
inline fun Component(block: ComponentBuilder.() -> Unit = {}): Component =
    ComponentBuilder().apply(block).build()

/**
 * Builder for a [Component]
 *
 * @see Component
 */
class ComponentBuilder {

    private val scopes = mutableSetOf<Any>()
    private val dependencies = mutableSetOf<Component>()
    private val bindings = mutableMapOf<Key, Binding<*>>()
    private val multiBindingMapBuilders = mutableMapOf<Key, MultiBindingMapBuilder<Any?, Any?>>()
    private val multiBindingSetBuilders = mutableMapOf<Key, MultiBindingSetBuilder<Any?>>()

    fun scopes(scope: Any) {
        check(scope !in scopes) { "Duplicated scope $scope" }
        this.scopes += scope
    }

    fun scopes(vararg scopes: Any) {
        scopes.forEach { scopes(it) }
    }

    /**
     * Scope the component
     *
     * @param scopes the scopes to include
     */
    fun scopes(vararg scopes: List<Any>) {
        scopes.forEach { scopes(it) }
    }

    fun dependencies(dependency: Component) {
        check(dependency !in dependencies) { "Duplicated dependency $dependency" }
        this.dependencies += dependency
    }

    fun dependencies(vararg dependencies: Component) {
        dependencies.forEach { dependencies(it) }
    }

    /**
     * Add component dependencies
     *
     * This all make all bindings of the dependencies accessible in this component
     *
     * @param dependencies the dependencies to add
     */
    fun dependencies(dependencies: List<Component>) {
        dependencies.forEach { dependencies(it) }
    }

    inline fun <reified T> factory(
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
        scoping: Scoping = Scoping.Unscoped,
        noinline definition: Definition<T>
    ): BindingContext<T> = factory(
        type = typeOf(),
        name = name,
        overrideStrategy = overrideStrategy,
        scoping = scoping,
        definition = definition
    )

    /**
     * Contributes a binding which will be instantiated on each request
     *
     * @param type the of the instance
     * @param name the name of the instance
     * @param overrideStrategy the strategy for handling overrides
     * @param scoping how instances should be scoped
     * @param definition the definitions which creates instances
     *
     * @see bind
     */
    fun <T> factory(
        type: Type<T>,
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
        scoping: Scoping = Scoping.Unscoped,
        definition: Definition<T>
    ): BindingContext<T> = bind(
        key = keyOf(type, name),
        binding = DefinitionBinding(definition = definition),
        overrideStrategy = overrideStrategy,
        scoping = scoping
    )

    inline fun <reified T> single(
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
        scoping: Scoping = Scoping.Scoped(),
        eager: Boolean = false,
        noinline definition: Definition<T>
    ): BindingContext<T> = single(
        type = typeOf(),
        name = name,
        overrideStrategy = overrideStrategy,
        scoping = scoping,
        eager = eager,
        definition = definition
    )

    /**
     * Contributes a binding which will be reused throughout the lifetime of the [Component] it life's in
     *
     * @param type the of the instance
     * @param name the name of the instance
     * @param overrideStrategy the strategy for handling overrides
     * @param scoping how instances should be scoped
     * @param eager whether the instance should be created when the [Component] get's created
     * @param definition the definitions which creates instances
     *
     * @see bind
     */
    fun <T> single(
        type: Type<T>,
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
        scoping: Scoping = Scoping.Scoped(),
        eager: Boolean = false,
        definition: Definition<T>
    ): BindingContext<T> =
        bind(
            key = keyOf(type, name),
            binding = DefinitionBinding(definition = definition).asSingle(),
            overrideStrategy = overrideStrategy,
            scoping = scoping,
            eager = eager,
            single = true
        )

    inline fun <reified T> instance(
        instance: T,
        name: Any? = null,
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
    ): BindingContext<T> {
        return instance(
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
        binding = InstanceBinding(instance),
        overrideStrategy = overrideStrategy,
        scoping = Scoping.Scoped()
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
        factory(type = type, name = UUID.randomUUID().toString()) { parameters ->
            get(type = type, name = name, parameters = parameters)
        }
            .block()
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
     * @param single whether instances are reused
     * @param eager whether a instance should be created when the [Component] get's created
     * @param scoping how instances should be scoped
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
        overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
        scoping: Scoping = Scoping.Unscoped,
        single: Boolean = false,
        eager: Boolean = false
    ): BindingContext<T> {
        binding.overrideStrategy = overrideStrategy
        binding.scoping = scoping
        binding.single = single
        binding.eager = eager

        if (overrideStrategy.check(
                existsPredicate = { key in bindings },
                errorMessage = { "Already declared binding for $key" }
            )
        ) {
            bindings[key] = binding
        }

        return BindingContext(binding = binding, key = key, componentBuilder = this)
    }

    /**
     * Create a new [Component] instance.
     */
    fun build(): Component {
        checkScopes()

        val dependencyBindings = dependencies
            .map { it.getAllBindings() }
            .fold(mutableMapOf<Key, Binding<*>>()) { acc, current ->
                current.forEach { (key, binding) ->
                    if (binding.overrideStrategy.check(
                            existsPredicate = { key in acc },
                            errorMessage = { "Already declared binding for $key" }
                        )
                    ) {
                        acc[key] = binding
                    }
                }

                return@fold acc
            }

        val scopedBindings = mutableMapOf<Key, Binding<*>>()
        val unlinkedUnscopedBindings = mutableMapOf<Key, UnlinkedBinding<*>>()
        val allMultiBindingMapBuilders = mutableMapOf<Key, MultiBindingMapBuilder<Any?, Any?>>()
        val allMultiBindingSetBuilders = mutableMapOf<Key, MultiBindingSetBuilder<Any?>>()

        fun addBinding(key: Key, binding: Binding<*>) {
            if (binding.overrideStrategy.check(
                    existsPredicate = {
                        key in scopedBindings ||
                                key in dependencyBindings
                    },
                    errorMessage = { "Already declared key $key" }
                )
            ) {
                scopedBindings[key] = binding
                if (binding.scoping is Scoping.Unscoped) unlinkedUnscopedBindings[key] =
                    binding as UnlinkedBinding<*>
            }
        }

        fun addUnlinkedUnscopedDependencyBinding(key: Key, binding: UnlinkedBinding<*>) {
            if (binding.overrideStrategy.check(
                    existsPredicate = {
                        key in scopedBindings ||
                                key in unlinkedUnscopedBindings
                    },
                    errorMessage = { "Already declared key $key" }
                )
            ) {
                unlinkedUnscopedBindings[key] = binding
            }
        }

        fun addMultiBindingMap(mapKey: Key, map: MultiBindingMap<Any?, Any?>) {
            val builder = allMultiBindingMapBuilders.getOrPut(mapKey) {
                MultiBindingMapBuilder(mapKey)
            }
            builder.putAll(map)
        }

        fun addMultiBindingSet(setKey: Key, set: MultiBindingSet<Any?>) {
            val builder = allMultiBindingSetBuilders.getOrPut(setKey) {
                MultiBindingSetBuilder(setKey)
            }

            builder.addAll(set)
        }

        dependencies.forEach { dependency ->
            dependency.unlinkedUnscopedBindings.forEach { (key, binding) ->
                addUnlinkedUnscopedDependencyBinding(key, binding)
            }
            dependency.multiBindingMaps.forEach { (mapKey, map) ->
                addMultiBindingMap(mapKey, map)
            }

            dependency.multiBindingSets.forEach { (setKey, set) ->
                addMultiBindingSet(setKey, set)
            }
        }

        bindings.forEach { (key, binding) -> addBinding(key, binding) }

        multiBindingMapBuilders.forEach {
            addMultiBindingMap(it.key, it.value.build())
        }

        multiBindingSetBuilders.forEach {
            addMultiBindingSet(it.key, it.value.build())
        }

        val multiBindingMaps = allMultiBindingMapBuilders.mapValues {
            it.value.build()
        }

        multiBindingMaps.forEach { (mapKey, map) ->
            includeMapBindings(scopedBindings, mapKey, map)
        }

        val multiBindingSets = allMultiBindingSetBuilders.mapValues {
            it.value.build()
        }
        multiBindingSets.forEach { (setKey, set) ->
            includeSetBindings(scopedBindings, setKey, set)
        }

        includeComponentBindings(scopedBindings)

        return Component(
            scopes = scopes,
            scopedBindings = scopedBindings,
            unlinkedUnscopedBindings = unlinkedUnscopedBindings,
            multiBindingMaps = multiBindingMaps,
            multiBindingSets = multiBindingSets,
            dependencies = dependencies
        )
    }

    private fun checkScopes() {
        val dependencyScopes = mutableSetOf<Any>()

        fun addScope(scope: Any) {
            check(scope !in dependencyScopes) {
                "Duplicated scope $scope"
            }

            dependencyScopes += scope
        }

        dependencies
            .flatMap { it.scopes }
            .forEach { addScope(it) }

        scopes.forEach { addScope(it) }
    }

    private fun includeComponentBindings(bindings: MutableMap<Key, Binding<*>>) {
        val componentBinding = ComponentBinding()
        componentBinding.scoping = Scoping.Scoped()
        componentBinding.overrideStrategy = OverrideStrategy.Permit
        val componentKey = keyOf<Component>()
        bindings[componentKey] = componentBinding
        scopes
            .map { keyOf<Component>(it) }
            .forEach { bindings[it] = componentBinding }
    }

    private class ComponentBinding : UnlinkedBinding<Component>() {
        override fun link(component: Component): LinkedBinding<Component> =
            Linked(component)

        private class Linked(private val component: Component) :
            LinkedBinding<Component>() {
            override fun invoke(parameters: Parameters): Component =
                component
        }
    }

    private fun includeMapBindings(
        bindings: MutableMap<Key, Binding<*>>,
        mapKey: Key,
        map: MultiBindingMap<*, *>
    ) {
        val bindingKeys = map
            .mapValues { it.value.key }

        val mapKeyType = mapKey.type.arguments[0]
        val mapValueType = mapKey.type.arguments[1]

        val mapOfProviderKey = keyOf(
            typeOf<Any?>(
                Map::class,
                arrayOf(
                    mapKeyType,
                    typeOf<Provider<*>>(Provider::class, arrayOf(mapValueType))
                )
            ),
            mapKey.name
        )

        bindings[mapOfProviderKey] = MapOfProviderBinding<Any?, Any?>(bindingKeys)
            .also { it.scoping = Scoping.Scoped() }

        val mapOfLazyKey = keyOf(
            typeOf<Any?>(
                Map::class,
                arrayOf(
                    mapKeyType,
                    typeOf<Lazy<*>>(Lazy::class, arrayOf(mapValueType))
                )
            ),
            mapKey.name
        )

        bindings[mapOfLazyKey] = MapOfLazyBinding<Any?, Any?>(mapOfProviderKey)
            .also { it.scoping = Scoping.Scoped() }

        bindings[mapKey] = MapOfValueBinding<Any?, Any?>(mapOfProviderKey)
            .also { it.scoping = Scoping.Scoped() }
    }

    private fun includeSetBindings(
        bindings: MutableMap<Key, Binding<*>>,
        setKey: Key,
        set: MultiBindingSet<*>
    ) {
        val setKeys = set.map { it.key }.toSet()

        val setElementType = setKey.type.arguments[0]

        val setOfProviderKey = keyOf(
            typeOf<Any?>(
                Set::class,
                arrayOf(
                    typeOf<Provider<*>>(Provider::class, arrayOf(setElementType))
                )
            ),
            setKey.name
        )

        bindings[setOfProviderKey] = SetOfProviderBinding<Any?>(setKeys)
            .also { it.scoping = Scoping.Scoped() }

        val setOfLazyKey = keyOf(
            typeOf<Any?>(
                Set::class,
                arrayOf(
                    typeOf<Lazy<*>>(Lazy::class, arrayOf(setElementType))
                )
            ),
            setKey.name
        )

        bindings[setOfLazyKey] = SetOfLazyBinding<Any?>(setOfProviderKey)
            .also { it.scoping = Scoping.Scoped() }

        bindings[setKey] = SetOfValueBinding<Any?>(setOfProviderKey)
            .also { it.scoping = Scoping.Scoped() }
    }

    private fun Component.getAllBindings(): Map<Key, Binding<*>> =
        mutableMapOf<Key, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key, Binding<*>>) {
        dependencies.forEach { it.collectBindings(bindings) }
        bindings += this.scopedBindings
    }
}

internal class InstanceBinding<T>(private val instance: T) : LinkedBinding<T>() {
    override fun invoke(parameters: Parameters) = instance
}
