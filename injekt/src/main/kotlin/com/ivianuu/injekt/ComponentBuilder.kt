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

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ComponentBuilder @PublishedApi internal constructor() {

    private val scopes = arrayListOf<KClass<out Annotation>>()
    private val modules = arrayListOf<Module>()
    private val dependencies = arrayListOf<Component>()

    fun scopes(scope: KClass<out Annotation>): ComponentBuilder {
        this.scopes += scope
        return this
    }

    fun scopes(vararg scopes: KClass<out Annotation>): ComponentBuilder {
        this.scopes += scopes
        return this
    }

    fun scopes(scopes: Iterable<KClass<out Annotation>>): ComponentBuilder {
        this.scopes += scopes
        return this
    }

    fun dependencies(dependency: Component): ComponentBuilder {
        this.dependencies += dependency
        return this
    }

    fun dependencies(vararg dependencies: Component): ComponentBuilder {
        this.dependencies += dependencies
        return this
    }

    fun dependencies(dependencies: Iterable<Component>): ComponentBuilder {
        this.dependencies += dependencies
        return this
    }

    fun modules(module: Module): ComponentBuilder {
        this.modules += module
        return this
    }

    fun modules(vararg modules: Module): ComponentBuilder {
        this.modules += modules
        return this
    }

    fun modules(modules: Iterable<Module>): ComponentBuilder {
        this.modules += modules
        return this
    }

    @PublishedApi
    internal fun build(): Component {
        checkScopes()

        val dependencyBindingKeys = dependencies
            .map { it.getAllBindingKeys() }
            .fold(hashSetOf<Key>()) { acc, current ->
                current.forEach { key ->
                    check(acc += key) {
                        "Already declared binding for $key"
                    }
                }

                return@fold acc
            }

        val allBindings = hashMapOf<Key, Binding<*>>()
        val unscopedBindings = hashMapOf<Key, Binding<*>>()

        var mapBindings: MapBindings? = null
        var setBindings: SetBindings? = null
        fun nonNullMapBindings(): MapBindings =
            mapBindings ?: MapBindings().also { mapBindings = it }

        fun nonNullSetBindings(): SetBindings =
            setBindings ?: SetBindings().also { setBindings = it }

        dependencies.forEach { dependency ->
            dependency.mapBindings?.let { nonNullMapBindings().putAll(it) }
            dependency.setBindings?.let { nonNullSetBindings().addAll(it) }
        }

        modules.forEach { module ->
            module.bindings.forEach { (key, binding) ->
                if ((key in allBindings
                            || key in dependencyBindingKeys) && !binding.override
                ) {
                    error("Already declared key $key")
                }
                allBindings[key] = binding
                if (binding.unscoped) {
                    unscopedBindings[key] = binding
                }
            }

            module.mapBindings?.let { nonNullMapBindings().putAll(it) }
            module.setBindings?.let { nonNullSetBindings().addAll(it) }
        }

        mapBindings?.getAll()?.forEach { (mapKey, map) ->
            includeMapBindings(allBindings, mapKey, map)
        }

        setBindings?.getAll()?.forEach { (setKey, set) ->
            includeSetBindings(allBindings, setKey, set)
        }

        includeComponentBindings(allBindings)

        val finalAllBindings = ConcurrentHashMap<Key, Binding<*>>()
        finalAllBindings += allBindings

        return Component(
            scopes, finalAllBindings, unscopedBindings,
            mapBindings, setBindings, dependencies
        )
    }

    private fun checkScopes() {
        val dependencyScopes = hashSetOf<KClass<out Annotation>>()

        dependencies
            .flatMap { it.scopes }
            .forEach {
                if (!dependencyScopes += it) {
                    error("Duplicated scope $it")
                }
            }

        scopes.forEach {
            if (!dependencyScopes += it) {
                error("Duplicated scope $it")
            }
        }

        check(scopes.isNotEmpty() || dependencyScopes.isEmpty()) {
            "Must have a scope if a dependency has a scope"
        }
    }

    private class ComponentBinding : UnlinkedBinding<Component>() {
        override fun link(linker: Linker): LinkedBinding<Component> = Linked(linker.component)

        private class Linked(private val component: Component) : LinkedBinding<Component>() {
            override fun invoke(parameters: ParametersDefinition?): Component =
                component
        }
    }

    private fun includeComponentBindings(bindings: MutableMap<Key, Binding<*>>) {
        val componentBinding = ComponentBinding()
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
        map: MapBindings.BindingMap<*, *>
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
        set: SetBindings.BindingSet<*>
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
        hashSetOf<Key>().also { collectBindingKeys(it) }

    private fun Component.collectBindingKeys(keys: MutableSet<Key>) {
        dependencies.forEach { it.collectBindingKeys(keys) }
        keys += this.allBindings.keys
    }

}

/**
 * Constructs a new [Component] which will configured [block]
 */
inline fun component(block: ComponentBuilder.() -> Unit = {}): Component =
    ComponentBuilder().apply(block).build()

inline fun <reified T : Annotation> ComponentBuilder.scopes(): ComponentBuilder {
    scopes(T::class)
    return this
}