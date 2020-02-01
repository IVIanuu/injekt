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
    private val modules = mutableSetOf<Module>()
    private val instances = mutableMapOf<Key, Binding<*>>()
    private val dependencies = mutableSetOf<Component>()

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

    fun modules(module: Module) {
        check(module !in modules) { "Duplicated module $module" }
        this.modules += module
    }

    fun modules(vararg modules: Module) {
        modules.forEach { modules(it) }
    }

    /**
     * Include all bindings of the modules in the component
     *
     * @param modules the modules to include
     *
     * @see Module
     */
    fun modules(modules: List<Module>) {
        modules.forEach { modules(it) }
    }

    inline fun <reified T> instance(
        instance: T,
        name: Any? = null,
        override: Boolean = false
    ) {
        instance(instance = instance, type = typeOf(), name = name, override = override)
    }

    /**
     * Adds a binding for a already existing instance
     * This is a shortcut to avoid the creation of a [Module] for just a single binding
     *
     * @param instance the instance to contribute
     * @param type the type for the [Key] by which the binding can be retrieved later in the [Component]
     * @param name the type for the [Key] by which the binding can be retrieved later in the [Component]
     */
    fun <T> instance(
        instance: T,
        type: Type<T>,
        name: Any? = null,
        override: Boolean = false
    ) {
        val key = keyOf(type, name)

        check(override || key !in instances) {
            "Already declared binding for $key"
        }

        val binding = InstanceBinding(instance)
        binding.override = override
        binding.scoped = true

        instances[key] = binding
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
                    check(key !in acc) {
                        "Already declared binding for $key"
                    }

                    acc[key] = binding
                }

                return@fold acc
            }

        val scopedBindings = mutableMapOf<Key, Binding<*>>()
        val unlinkedUnscopedBindings = mutableMapOf<Key, UnlinkedBinding<*>>()
        val multiBindingMapBuilders = mutableMapOf<Key, MultiBindingMapBuilder<Any?, Any?>>()
        val multiBindingSetBuilders = mutableMapOf<Key, MultiBindingSetBuilder<Any?>>()

        fun addBinding(key: Key, binding: Binding<*>) {
            check(
                binding.override || (key !in scopedBindings &&
                        key !in dependencyBindings)
            ) {
                "Already declared key $key"
            }
            scopedBindings[key] = binding
            if (!binding.scoped) unlinkedUnscopedBindings[key] = binding as UnlinkedBinding<*>
        }

        fun addUnlinkedUnscopedDependencyBinding(key: Key, binding: UnlinkedBinding<*>) {
            check(
                binding.override || (key !in scopedBindings &&
                        key !in unlinkedUnscopedBindings)
            ) {
                "Already declared key $key"
            }
            unlinkedUnscopedBindings[key] = binding
        }

        fun addMultiBindingMap(mapKey: Key, map: MultiBindingMap<Any?, Any?>) {
            val builder = multiBindingMapBuilders.getOrPut(mapKey) {
                MultiBindingMapBuilder(mapKey)
            }
            builder.putAll(map)
        }

        fun addMultiBindingSet(setKey: Key, set: MultiBindingSet<Any?>) {
            val builder = multiBindingSetBuilders.getOrPut(setKey) {
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

        instances.forEach { (key, binding) -> addBinding(key, binding) }

        modules.forEach { module ->
            module.bindings.forEach { (key, binding) ->
                addBinding(key, binding)
            }

            module.multiBindingMaps.forEach { (mapKey, map) ->
                addMultiBindingMap(mapKey, map)
            }

            module.multiBindingSets.forEach { (setKey, set) ->
                addMultiBindingSet(setKey, set)
            }
        }

        val multiBindingMaps = multiBindingMapBuilders.mapValues {
            it.value.build()
        }

        multiBindingMaps.forEach { (mapKey, map) ->
            includeMapBindings(scopedBindings, mapKey, map)
        }

        val multiBindingSets = multiBindingSetBuilders.mapValues {
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

        check(scopes.isNotEmpty() || dependencyScopes.isEmpty()) {
            "Must have a scope if a dependency has a scope"
        }
    }

    private fun includeComponentBindings(bindings: MutableMap<Key, Binding<*>>) {
        val componentBinding = ComponentBinding()
        componentBinding.scoped = true
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
            override fun invoke(parameters: Parameters?): Component =
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
            .also { it.scoped = true }

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
            .also { it.scoped = true }

        bindings[mapKey] = MapOfValueBinding<Any?, Any?>(mapOfProviderKey)
            .also { it.scoped = true }
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
            .also { it.scoped = true }

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
            .also { it.scoped = true }

        bindings[setKey] = SetOfValueBinding<Any?>(setOfProviderKey)
            .also { it.scoped = true }
    }

    private fun Component.getAllBindings(): Map<Key, Binding<*>> =
        mutableMapOf<Key, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key, Binding<*>>) {
        dependencies.forEach { it.collectBindings(bindings) }
        bindings += this.scopedBindings
    }
}
