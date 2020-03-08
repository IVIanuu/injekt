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
 * Create a [Component] configured by [block]
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

    private val scopes = mutableListOf<Scope>()
    private val dependencies = mutableListOf<Component>()
    private val bindings = mutableMapOf<Key<*>, Binding<*>>()
    private val multiBindingMapBuilders = mutableMapOf<Key<*>, MultiBindingMapBuilder<Any?, Any?>>()
    private val multiBindingSetBuilders = mutableMapOf<Key<*>, MultiBindingSetBuilder<Any?>>()

    /**
     * Adds the [scopes] this allows generated [Binding]s
     * to be associated with components.
     *
     * @see ScopeMarker
     */
    fun scopes(vararg scopes: Scope) {
        scopes.forEach { scope ->
            check(scope !in this.scopes) { "Duplicated scope $scope" }
            this.scopes += scope
        }
    }

    /**
     * Adds the [dependencies] to the component if this component cannot resolve a instance
     * it will ask it's dependencies
     */
    fun dependencies(vararg dependencies: Component) {
        dependencies.forEach { dependency ->
            check(dependency !in this.dependencies) { "Duplicated dependency $dependency" }
            this.dependencies += dependency
        }
    }

    inline fun <reified S, reified T> alias(
        originalQualifiers: Qualifier = Qualifier.None,
        aliasQualifiers: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = alias<S, T>(
        originalKey = keyOf(qualifier = originalQualifiers),
        aliasKey = keyOf(qualifier = aliasQualifiers),
        duplicateStrategy = duplicateStrategy
    )

    /**
     * Makes the [Binding] for [originalKey] retrievable via [aliasKey]
     *
     * For example the following code points the Repository request to RepositoryImpl
     *
     * ´´´
     * val component = Component {
     *     factory { RepositoryImpl() }
     *     alias<RepositoryImpl, Repository>()
     * }
     *
     * val repository = component.get<Repository>()
     *
     * ´´´
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
        qualifier: Qualifier = Qualifier.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
    ): BindingContext<T> = instance(
        instance = instance,
        key = keyOf(qualifier = qualifier),
        duplicateStrategy = duplicateStrategy
    )

    /**
     * Adds the [instance] as a binding for [key]
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
        qualifier: Qualifier = Qualifier.None,
        noinline block: BindingContext<T>.() -> Unit
    ) {
        withBinding(key = keyOf(qualifier = qualifier), block = block)
    }

    /**
     * Runs the [block] in the [BindingContext] of the [Binding] for [key]
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
     */
    fun <T> withBinding(
        key: Key<T>,
        block: BindingContext<T>.() -> Unit
    ) {
        // we create a proxy binding which links to the original binding
        // because we have no reference to the original one it's likely in another [Module] or [Component]
        // we use a unique id here to make sure that the binding does not collide with any user config
        factory(key = key.copy(qualifier = UUIDQualifier())) { parameters ->
            get(key, parameters = parameters)
        }.block()
    }

    private data class UUIDQualifier(private val uuid: UUID = UUID.randomUUID()) : Qualifier.Element

    inline fun <reified K, reified V> map(
        mapQualifiers: Qualifier = Qualifier.None,
        noinline block: MultiBindingMapBuilder<K, V>.() -> Unit = {}
    ) {
        map(
            mapKey = keyOf(
                classifier = Map::class,
                arguments = arrayOf(
                    keyOf<K>(),
                    keyOf<V>()
                ),
                qualifier = mapQualifiers
            ), block = block
        )
    }

    /**
     * Runs the [block] in the scope of the [MultiBindingMapBuilder] for [mapKey]
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
        setQualifiers: Qualifier = Qualifier.None,
        noinline block: MultiBindingSetBuilder<E>.() -> Unit = {}
    ) {
        set(
            setKey = keyOf(
                classifier = Set::class,
                arguments = arrayOf(keyOf<E>()),
                qualifier = setQualifiers
            ),
            block = block
        )
    }

    /**
     * Runs the [block] in the scope of the [MultiBindingSetBuilder] for [setKey]
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
     * Adds the [binding]
     * This function is rarely used directly instead use [factory] or [single]
     *
     * @see factory
     * @see single
     * @see BindingContext
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
        val dependencyScopes = mutableSetOf<Scope>()

        fun addScope(scope: Scope) {
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
            behavior = BoundBehavior(),
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = { this }
        )

        bindings[componentBinding.key] = componentBinding
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
            behavior = BoundBehavior(),
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = {
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
            qualifier = mapKey.qualifier
        )
        bindings[mapOfProviderKey] = Binding(
            key = mapOfProviderKey,
            behavior = BoundBehavior(),
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = {
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
            qualifier = mapKey.qualifier
        )
        bindings[mapOfLazyKey] = Binding(
            key = mapOfLazyKey,
            behavior = BoundBehavior(),
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = {
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
            qualifier = setKey.qualifier
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
            qualifier = setKey.qualifier
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
