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

import kotlin.reflect.KClass

/**
 * Construct a [Component] with a lambda
 *
 * @param block the block to configure the component
 * @return the constructed [Component]
 *
 * @see Component
 */
fun component(block: ComponentBuilder.() -> Unit = {}): Component =
    ComponentBuilder().apply(block).build()

/**
 * Builder for an [Component]
 *
 * @see component
 * @see Component
 */
class ComponentBuilder internal constructor() {

    private val scopes = mutableListOf<KClass<out Annotation>>()
    private val modules = mutableListOf<Module>()
    private val instances = mutableMapOf<Key, Binding<*>>()
    private val dependencies = mutableListOf<Component>()

    inline fun <reified T : Annotation> scopes() {
        scopes(T::class)
    }

    fun scopes(scope: KClass<out Annotation>) {
        this.scopes += scope
    }

    fun scopes(vararg scopes: KClass<out Annotation>) {
        this.scopes += scopes
    }

    /**
     * Scope the component
     *
     * @param scopes the scopes to include
     */
    fun scopes(scopes: List<KClass<out Annotation>>) {
        this.scopes += scopes
    }

    fun dependencies(dependency: Component) {
        this.dependencies += dependency
    }

    fun dependencies(vararg dependencies: Component) {
        this.dependencies += dependencies
    }

    /**
     * Add component dependencies
     *
     * This all make all bindings of the dependencies accessible in this component
     *
     * @param dependencies the dependencies to add
     */
    fun dependencies(dependencies: List<Component>) {
        this.dependencies += dependencies
    }

    fun modules(module: Module) {
        this.modules += module
    }

    fun modules(vararg modules: Module) {
        this.modules += modules
    }

    /**
     * Include all bindings of the modules in the component
     *
     * @param modules the modules to include
     *
     * @see Module
     */
    fun modules(modules: List<Module>) {
        this.modules += modules
    }

    /**
     * Adds a binding for a already existing instance
     * This is a shortcut to avoid the creation of a [Module] for just a single binding
     *
     * @param instance the instance to contribute
     * @param type the type for the [Key] by which the binding can be retrieved later in the component
     * @param name the type for the [Key] by which the binding can be retrieved later in the component
     */
    fun <T> instance(
        instance: T,
        type: Type<T> = typeOf(instance),
        name: Any? = null,
        override: Boolean = false
    ) {
        val key = keyOf(type, name)

        check(key !in instances || override) {
            "Already declared binding for $key"
        }

        val binding = LinkedInstanceBinding(instance)
        binding.override = override
        binding.unscoped = false

        instances[key] = binding
    }

    internal fun build(): Component {
        checkScopes()

        val dependencyBindingKeys = dependencies
            .map { it.getAllBindingKeys() }
            .fold(mutableSetOf<Key>()) { acc, current ->
                current.forEach { key ->
                    check(acc.add(key)) {
                        "Already declared binding for $key"
                    }
                }

                return@fold acc
            }

        val allBindings = mutableMapOf<Key, Binding<*>>()
        val unscopedBindings = mutableMapOf<Key, Binding<*>>()
        val eagerBindings = mutableListOf<Key>()
        val mapBindings = MapBindings()
        val setBindings = SetBindings()

        dependencies.forEach { dependency ->
            mapBindings.putAll(dependency.mapBindings)
            setBindings.addAll(dependency.setBindings)
        }

        instances.forEach { (key, binding) ->
            check(
                (key !in allBindings
                        && key !in dependencyBindingKeys) || binding.override
            ) {
                "Already declared key $key"
            }
            allBindings[key] = binding
            if (binding.unscoped) {
                unscopedBindings[key] = binding
            }
        }

        modules.forEach { module ->
            module.bindings.forEach { (key, binding) ->
                check(
                    (key !in allBindings
                            && key !in dependencyBindingKeys) || binding.override
                ) {
                    "Already declared key $key"
                }
                allBindings[key] = binding
                if (binding.unscoped) unscopedBindings[key] = binding
                if (binding.eager) eagerBindings += key
            }

            mapBindings.putAll(module.mapBindings)
            setBindings.addAll(module.setBindings)
        }

        mapBindings.getAll().forEach { (mapKey, map) ->
            includeMapBindings(allBindings, mapKey, map)
        }

        setBindings.getAll().forEach { (setKey, set) ->
            includeSetBindings(allBindings, setKey, set)
        }

        includeComponentBindings(allBindings)

        return Component(
            scopes = scopes,
            allBindings = allBindings,
            unlinkedUnscopedBindings = unscopedBindings,
            eagerBindings = eagerBindings,
            mapBindings = mapBindings,
            setBindings = setBindings,
            dependencies = dependencies
        )
    }

    private fun checkScopes() {
        val dependencyScopes = mutableSetOf<KClass<out Annotation>>()

        dependencies
            .flatMap { it.scopes }
            .forEach {
                if (!dependencyScopes.add(it)) {
                    error("Duplicated scope $it")
                }
            }

        scopes.forEach {
            if (!dependencyScopes.add(it)) {
                error("Duplicated scope $it")
            }
        }

        check(scopes.isNotEmpty() || dependencyScopes.isEmpty()) {
            "Must have a scope if a dependency has a scope"
        }
    }

    private fun includeComponentBindings(bindings: MutableMap<Key, Binding<*>>) {
        val componentBinding = UnlinkedComponentBinding()
        componentBinding.unscoped = false
        val componentKey = keyOf<Component>()
        bindings[componentKey] = componentBinding
        scopes
            .map { keyOf<Component>(it) }
            .forEach { bindings[it] = componentBinding }
    }

    private fun includeMapBindings(
        bindings: MutableMap<Key, Binding<*>>,
        mapKey: Key,
        map: BindingMap<*, *>
    ) {
        val bindingKeys = map.getBindingMap() as Map<Any?, Key>

        val mapKeyType = mapKey.type.parameters[0]
        val mapValueType = mapKey.type.parameters[1]

        val mapOfProviderKey = keyOf(
            typeOf<Any?>(
                Map::class,
                mapKeyType,
                typeOf<Provider<*>>(Provider::class, mapValueType)
            ),
            mapKey.name
        )

        bindings[mapOfProviderKey] = UnlinkedMapOfProviderBinding<Any?, Any?>(bindingKeys)
            .also { it.unscoped = false }

        val mapOfLazyKey = keyOf(
            typeOf<Any?>(
                Map::class,
                mapKeyType,
                typeOf<Lazy<*>>(Lazy::class, mapValueType)
            ),
            mapKey.name
        )

        bindings[mapOfLazyKey] = UnlinkedMapOfLazyBinding<Any?, Any?>(mapOfProviderKey)
            .also { it.unscoped = false }

        bindings[mapKey] = UnlinkedMapOfValueBinding<Any?, Any?>(mapOfProviderKey)
            .also { it.unscoped = false }
    }

    private fun includeSetBindings(
        bindings: MutableMap<Key, Binding<*>>,
        setKey: Key,
        set: BindingSet<*>
    ) {
        val setKeys = set.getBindingSet()

        val setElementType = setKey.type.parameters[0]

        val setOfProviderKey = keyOf(
            typeOf<Any?>(
                Set::class,
                typeOf<Provider<*>>(Provider::class, setElementType)
            ),
            setKey.name
        )

        bindings[setOfProviderKey] = UnlinkedSetOfProviderBinding<Any?>(setKeys)
            .also { it.unscoped = false }

        val setOfLazyKey = keyOf(
            typeOf<Any?>(
                Set::class,
                typeOf<Lazy<*>>(Lazy::class, setElementType)
            ),
            setKey.name
        )

        bindings[setOfLazyKey] = UnlinkedSetOfLazyBinding<Any?>(setOfProviderKey)
            .also { it.unscoped = false }

        bindings[setKey] = UnlinkedSetOfValueBinding<Any?>(setOfProviderKey)
            .also { it.unscoped = false }
    }

    private fun Component.getAllBindingKeys(): Set<Key> =
        mutableSetOf<Key>().also { collectBindingKeys(it) }

    private fun Component.collectBindingKeys(keys: MutableSet<Key>) {
        dependencies.forEach { it.collectBindingKeys(keys) }
        keys += this.allBindings.keys
    }

}