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

    private val scopes = mutableListOf<Any>()
    private val dependencies = mutableListOf<Component>()
    private val bindings = mutableMapOf<Key<*>, Binding<*>>()
    private val multiBindingMapBuilders = mutableMapOf<Key<*>, MultiBindingMapBuilder<Any?, Any?>>()
    private val multiBindingSetBuilders = mutableMapOf<Key<*>, MultiBindingSetBuilder<Any?>>()

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
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        bound: Boolean = false,
        noinline provider: BindingProvider<T>
    ): BindingContext<T> = factory(
        key = keyOf(name = name),
        duplicateStrategy = duplicateStrategy,
        bound = bound,
        provider = provider
    )

    /**
     * Contributes a binding which will be instantiated on each request
     *
     * @param key the key to retrieve the instance
     * @param duplicateStrategy the strategy for handling overrides
     * @param bound whether instances should be created in the scope of the component
     * @param provider the definitions which creates instances
     *
     * @see add
     */
    fun <T> factory(
        key: Key<T>,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        bound: Boolean = false,
        provider: BindingProvider<T>
    ): BindingContext<T> = add(
        Binding(
            key = key,
            duplicateStrategy = duplicateStrategy,
            provider = if (bound) BoundProvider(provider = provider) else provider
        )
    )

    inline fun <reified T> single(
        name: Any? = null,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        eager: Boolean = false,
        noinline provider: BindingProvider<T>
    ): BindingContext<T> = single(
        key = keyOf(name = name),
        duplicateStrategy = duplicateStrategy,
        eager = eager,
        provider = provider
    )

    /**
     * Contributes a binding which will be reused throughout the lifetime of the [Component] it life's in
     *
     * @param key the key to retrieve the instance
     * @param duplicateStrategy the strategy for handling overrides
     * @param eager whether the instance should be created when the [Component] get's created
     * @param provider the definitions which creates instances
     *
     * @see add
     */
    fun <T> single(
        key: Key<T>,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        eager: Boolean = false,
        provider: BindingProvider<T>
    ): BindingContext<T> = add(
        Binding(
            key = key,
            duplicateStrategy = duplicateStrategy,
            provider = SingleProvider(provider = provider)
                .let { if (eager) EagerProvider(it) else it }
        )
    )

    inline fun <reified S, reified T> alias(
        originalName: Any? = null,
        aliasName: Any? = null,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = alias<S, T>(
        originalKey = keyOf(name = originalName),
        aliasKey = keyOf(name = aliasName),
        duplicateStrategy = duplicateStrategy
    )

    /**
     * Makes the [Binding] for [originalKey] retrievable via [aliasKey]
     *
     * For example the following code binds RepositoryImpl to Repository
     *
     * ´´´
     * factory { RepositoryImpl() }
     * alias<RepositoryImpl, Repository>()
     * ´´´
     *
     * @param originalKey the key of the original binding
     * @param duplicateStrategy how overrides should be handled
     *
     */
    fun <S, T> alias(
        originalKey: Key<S>,
        aliasKey: Key<T>,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = factory(
        key = aliasKey,
        duplicateStrategy = duplicateStrategy
    ) { parameters -> get(originalKey, parameters = parameters) as T }

    inline fun <reified T> instance(
        instance: T,
        name: Any? = null,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = instance(
        instance = instance,
        key = keyOf(name = name),
        duplicateStrategy = duplicateStrategy
    )

    /**
     * Adds a binding for a already existing instance
     *
     * @param instance the instance to contribute
     * @param key the key to retrieve the instance
     * @return the [BindingContext] to chain binding calls
     *
     * @see add
     */
    fun <T> instance(
        instance: T,
        key: Key<T>,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = add(
        Binding(
            key = key,
            duplicateStrategy = duplicateStrategy,
            provider = { instance }
        )
    )

    inline fun <reified T> withBinding(
        name: Any? = null,
        noinline block: BindingContext<T>.() -> Unit
    ) {
        withBinding(key = keyOf(name = name), block = block)
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
     * withBinding(key = keyOf<MyRepository>()) {
     *     bindAlias<Repository>()
     * }
     *
     * ´´´
     *
     * @param key the type of the binding
     * @param block the lambda to call in the context of the other binding
     */
    fun <T> withBinding(
        key: Key<T>,
        block: BindingContext<T>.() -> Unit
    ) {
        // we create a proxy binding which links to the original binding
        // because we have no reference to the original one it's likely in another [Module] or [Component]
        // we use a unique id here to make sure that the binding does not collide with any user config
        factory(key = key.copy(name = UUID.randomUUID().toString())) { parameters ->
            get(key, parameters = parameters)
        }.block()
    }

    inline fun <reified K, reified V> map(
        mapName: Any? = null,
        noinline block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
    ) {
        map(
            mapKey = keyOf(
                classifier = Map::class,
                arguments = arrayOf(
                    keyOf<K>(),
                    keyOf<V>()
                ),
            name = mapName
            ), block = block
        )
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
        mapKey: Key<Map<K, V>>,
        block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
    ) {
        val builder = multiBindingMapBuilders.getOrPut(mapKey) {
            MultiBindingMapBuilder(mapKey) as MultiBindingMapBuilder<Any?, Any?>
        } as MultiBindingMapBuilder<K, V>

        builder.apply(block)
    }

    inline fun <reified E> set(
        setName: Any? = null,
        noinline block: MultiBindingSetBuilder<E>.() -> Unit = {}
    ) {
        set(
            setKey = keyOf(
                classifier = Set::class,
                arguments = arrayOf(keyOf<E>()),
                name = setName
            ),
            block = block
        )
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
        setKey: Key<Set<E>>,
        block: MultiBindingSetBuilder<E>.() -> Unit = {}
    ) {
        val builder = multiBindingSetBuilders.getOrPut(setKey) {
            MultiBindingSetBuilder(setKey) as MultiBindingSetBuilder<Any?>
        } as MultiBindingSetBuilder<E>

        builder.apply(block)
    }

    /**
     * Contributes the binding
     * This function is rarely used directly instead use [factory] or [single]
     *
     * @param binding the binding to add
     *
     * @see Component.get
     * @see BindingContext
     * @see factory
     * @see single
     */
    fun <T> add(binding: Binding<T>): BindingContext<T> {
        if (binding.duplicateStrategy.check(
                existsPredicate = { binding.key in bindings },
                errorMessage = { "Already declared binding for ${binding.key}" }
            )
        ) {
            bindings[binding.key] = binding
        }

        return BindingContext(binding = binding, componentBuilder = this)
    }

    /**
     * Create a new [Component] instance.
     */
    fun build(): Component {
        checkScopes()

        val dependencyBindings = dependencies
            .map { it.getAllBindings() }
            .fold(mutableMapOf<Key<*>, Binding<*>>()) { acc, current ->
                current.forEach { (key, binding) ->
                    if (binding.duplicateStrategy.check(
                            existsPredicate = { key in acc },
                            errorMessage = { "Already declared binding for $key" }
                        )
                    ) {
                        acc[key] = binding
                    }
                }

                return@fold acc
            }

        val finalBindings = mutableMapOf<Key<*>, Binding<*>>()
        val allMultiBindingMapBuilders = mutableMapOf<Key<*>, MultiBindingMapBuilder<Any?, Any?>>()
        val allMultiBindingSetBuilders = mutableMapOf<Key<*>, MultiBindingSetBuilder<Any?>>()

        fun addBinding(binding: Binding<*>) {
            if (binding.duplicateStrategy.check(
                    existsPredicate = { binding.key in dependencyBindings },
                    errorMessage = { "Already declared key ${binding.key}" })
            ) {
                finalBindings[binding.key] = binding
            }
        }

        fun addMultiBindingMap(mapKey: Key<*>, map: MultiBindingMap<*, *>) {
            val builder = allMultiBindingMapBuilders.getOrPut(mapKey) {
                MultiBindingMapBuilder(mapKey as Key<Map<Any?, Any?>>)
            }
            builder.putAll(map as MultiBindingMap<Any?, Any?>)
        }

        fun addMultiBindingSet(setKey: Key<*>, set: MultiBindingSet<*>) {
            val builder = allMultiBindingSetBuilders.getOrPut(setKey) {
                MultiBindingSetBuilder(setKey as Key<Set<Any?>>)
            }

            builder.addAll(set)
        }

        dependencies.forEach { dependency ->
            dependency.multiBindingMaps.forEach { (mapKey, map) ->
                addMultiBindingMap(mapKey, map)
            }

            dependency.multiBindingSets.forEach { (setKey, set) ->
                addMultiBindingSet(setKey, set)
            }
        }

        bindings.values.forEach { binding -> addBinding(binding) }

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
            includeMapBindings(finalBindings, mapKey, map)
        }

        val multiBindingSets = allMultiBindingSetBuilders.mapValues {
            it.value.build()
        }
        multiBindingSets.forEach { (setKey, set) ->
            includeSetBindings(finalBindings, setKey, set)
        }

        includeComponentBindings(finalBindings)

        return Component(
            scopes = scopes,
            dependencies = dependencies,
            bindings = finalBindings,
            multiBindingMaps = multiBindingMaps,
            multiBindingSets = multiBindingSets
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

    private fun includeComponentBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        val componentBinding = Binding(
            key = keyOf(),
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = ComponentProvider(null)
        )

        bindings[componentBinding.key] = componentBinding
        scopes
            .forEach {
                bindings[keyOf<Component>(it)] = Binding(
                    key = keyOf(),
                    duplicateStrategy = DuplicateStrategy.Permit,
                    provider = ComponentProvider(it)
                )
            }
    }

    private class ComponentProvider(
        private val scope: Any?
    ) : (Component, Parameters) -> Component, ComponentInitObserver {
        private lateinit var component: Component
        override fun onInit(component: Component) {
            check(!this::component.isInitialized) {
                "Already scoped to $component"
            }

            this.component = if (scope == null) component
            else component.getComponentForScope(scope)
        }

        override fun invoke(p1: Component, p2: Parameters): Component = component
    }

    private fun includeMapBindings(
        bindings: MutableMap<Key<*>, Binding<*>>,
        mapKey: Key<*>,
        map: MultiBindingMap<*, *>
    ) {
        val bindingKeys = map
            .mapValues { it.value.key }

        val mapKeyKey = mapKey.arguments[0]
        val mapValueKey = mapKey.arguments[1]

        bindings[mapKey] = Binding(
            key = mapKey as Key<Map<*, *>>,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = BoundProvider {
                bindingKeys
                    .mapValues { get(key = it.value) }
            }
        )

        val mapOfProviderKey = keyOf<Map<*, Provider<*>>>(
            classifier = Map::class,
            arguments = arrayOf(
                mapKeyKey,
                keyOf<Provider<Any?>>(
                    classifier = Provider::class,
                    arguments = arrayOf(mapValueKey)
                )
            ),
            name = mapKey.name
        )
        bindings[mapOfProviderKey] = Binding(
            key = mapOfProviderKey,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = BoundProvider {
                bindingKeys
                    .mapValues { KeyedProvider(this, it.value) }
            }
        )

        val mapOfLazyKey = keyOf<Map<*, Lazy<*>>>(
            classifier = Map::class,
            arguments = arrayOf(
                mapKeyKey,
                keyOf<Lazy<Any?>>(
                    classifier = Lazy::class,
                    arguments = arrayOf(mapValueKey)
                )
            ),
            name = mapKey.name
        )
        bindings[mapOfLazyKey] = Binding(
            key = mapOfLazyKey,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = BoundProvider {
                bindingKeys
                    .mapValues { KeyedLazy(this, it.value) }
            }
        )
    }

    private fun includeSetBindings(
        bindings: MutableMap<Key<*>, Binding<*>>,
        setKey: Key<*>,
        set: MultiBindingSet<*>
    ) {
        val setKeys = set.mapTo(mutableSetOf()) { it.key }

        val setElementKey = setKey.arguments[0]

        bindings[setKey] = Binding(
            key = setKey as Key<Set<*>>,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = {
                setKeys
                    .mapTo(mutableSetOf()) { get(key = it) }
            }
        )

        val setOfProviderKey = keyOf<Set<Provider<*>>>(
            classifier = Set::class,
            arguments = arrayOf(
                keyOf<Provider<*>>(
                    classifier = Provider::class,
                    arguments = arrayOf(setElementKey)
                )
            ),
            name = setKey.name
        )
        bindings[setOfProviderKey] = Binding(
            key = setOfProviderKey,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = {
                setKeys
                    .mapTo(mutableSetOf()) {
                        KeyedProvider(this, it)
                    }
            }
        )

        val setOfLazyKey = keyOf<Set<Lazy<*>>>(
            classifier = Set::class,
            arguments = arrayOf(
                keyOf<Lazy<*>>(
                    classifier = Lazy::class,
                    arguments = arrayOf(setElementKey)
                )
            ),
            name = setKey.name
        )
        bindings[setOfLazyKey] = Binding(
            key = setOfLazyKey,
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = {
                setKeys
                    .mapTo(mutableSetOf()) {
                        KeyedLazy(this, it)
                    }
            }
        )
    }

    private fun Component.getAllBindings(): Map<Key<*>, Binding<*>> =
        mutableMapOf<Key<*>, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        dependencies.forEach { it.collectBindings(bindings) }
        bindings += this.bindings
    }
}
