/*
 * Copyright 2018 Manuel Wrage
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
        this.scopes.add(scope)
        return this
    }

    fun scopes(vararg scopes: KClass<out Annotation>): ComponentBuilder {
        this.scopes.addAll(scopes)
        return this
    }

    fun scopes(scopes: Iterable<KClass<out Annotation>>): ComponentBuilder {
        this.scopes.addAll(scopes)
        return this
    }

    fun dependencies(dependency: Component): ComponentBuilder {
        this.dependencies.add(dependency)
        return this
    }

    fun dependencies(vararg dependencies: Component): ComponentBuilder {
        this.dependencies.addAll(dependencies)
        return this
    }

    fun dependencies(dependencies: Iterable<Component>): ComponentBuilder {
        this.dependencies.addAll(dependencies)
        return this
    }

    fun modules(module: Module): ComponentBuilder {
        this.modules.add(module)
        return this
    }

    fun modules(vararg modules: Module): ComponentBuilder {
        this.modules.addAll(modules)
        return this
    }

    fun modules(modules: Iterable<Module>): ComponentBuilder {
        this.modules.addAll(modules)
        return this
    }

    @PublishedApi
    internal fun build(): Component {
        checkScopes()

        val dependencyBindingKeys = dependencies
            .map { it.getAllBindingKeys() }
            .fold(hashSetOf<Key>()) { acc, current ->
                current.forEach { key ->
                    check(acc.add(key)) {
                        "Already declared binding for $key"
                    }
                }

                return@fold acc
            }

        val bindings = hashMapOf<Key, Binding<*>>()

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
                if ((bindings.contains(key)
                            || dependencyBindingKeys.contains(key)) && !binding.override
                ) {
                    error("Already declared key $key")
                }
                bindings[key] = binding
            }

            module.mapBindings?.let { nonNullMapBindings().putAll(it) }
            module.setBindings?.let { nonNullSetBindings().addAll(it) }
        }

        mapBindings?.getAll()?.forEach { (mapKey, map) ->
            includeMapBindings(bindings, mapKey, map)
        }

        setBindings?.getAll()?.forEach { (setKey, set) ->
            includeSetBindings(bindings, setKey, set)
        }

        val finalBindings = ConcurrentHashMap<Key, Binding<*>>()
        finalBindings.putAll(bindings)

        return Component(scopes, finalBindings, mapBindings, setBindings, dependencies)
    }

    private fun checkScopes() {
        val dependencyScopes = hashSetOf<KClass<out Annotation>>()

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

    private fun includeMapBindings(
        bindings: MutableMap<Key, Binding<*>>,
        mapKey: Key,
        map: MapBindings.BindingMap<*, *>
    ) {
        val bindingKeys = map.getBindingMap() as Map<Any?, Key>
        bindings[mapKey] = UnlinkedMapBinding<Any?, Any?>(bindingKeys)

        val lazyMapKey = keyOf(
            typeOf<Any?>(
                Map::class, mapKey.type.parameters[0],
                typeOf<Any?>(Lazy::class, mapKey.type.parameters[1])
            ),
            mapKey.name
        )
        val lazyBindingKeys = bindingKeys.mapValues {
            keyOf(
                typeOf<Any?>(Lazy::class, it.value.type),
                it.value.name
            )
        }
        bindings[lazyMapKey] = UnlinkedMapBinding<Any?, Any?>(lazyBindingKeys)

        val providerMapKey = keyOf(
            typeOf<Any?>(
                Map::class, mapKey.type.parameters[0],
                typeOf<Any?>(Provider::class, mapKey.type.parameters[1])
            ),
            mapKey.name
        )
        val providerBindingKeys = bindingKeys.mapValues {
            keyOf(
                typeOf<Any?>(Provider::class, it.value.type),
                it.value.name
            )
        }
        bindings[providerMapKey] = UnlinkedMapBinding<Any?, Any?>(providerBindingKeys)
    }

    private fun includeSetBindings(
        bindings: MutableMap<Key, Binding<*>>,
        setKey: Key,
        set: SetBindings.BindingSet<*>
    ) {
        val setKeys = set.getBindingSet()
        bindings[setKey] = UnlinkedSetBinding<Any?>(setKeys)

        val lazySetKey = keyOf(
            typeOf<Any?>(Set::class, typeOf<Any?>(Lazy::class, setKey.type.parameters[0])),
            setKey.name
        )
        val lazySetKeys = setKeys
            .map {
                keyOf(
                    typeOf<Any?>(Lazy::class, it.type),
                    it.name
                )
            }
            .toSet()
        bindings[lazySetKey] = UnlinkedSetBinding<Any?>(lazySetKeys)

        val providerSetKey = keyOf(
            typeOf<Any?>(Set::class, typeOf<Any?>(Provider::class, setKey.type.parameters[0])),
            setKey.name
        )
        val providerSetKeys = setKeys
            .map {
                keyOf(
                    typeOf<Any?>(Provider::class, it.type),
                    it.name
                )
            }
            .toSet()
        bindings[providerSetKey] = UnlinkedSetBinding<Any?>(providerSetKeys)
    }

    private fun Component.getAllBindingKeys(): Set<Key> =
        hashSetOf<Key>().also { collectBindingKeys(it) }

    private fun Component.collectBindingKeys(keys: MutableSet<Key>) {
        dependencies.forEach { it.collectBindingKeys(keys) }
        keys.addAll(this.bindings.filterValues { !it.isPrivate }.keys)
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