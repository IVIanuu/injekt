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

class ComponentBuilder @PublishedApi internal constructor() {

    var scope: Scope? = null

    private val modules = mutableListOf<Module>()
    private val dependencies = mutableListOf<Component>()

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
    internal fun build(): Component = createComponent(
        scope = scope,
        modules = modules,
        dependencies = dependencies
    )

}

/**
 * Constructs a new [Component] which will configured [block]
 */
inline fun component(block: ComponentBuilder.() -> Unit = {}): Component =
    ComponentBuilder().apply(block).build()

@PublishedApi
internal fun createComponent(
    scope: Scope? = null,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component {
    // todo clean up the whole function

    val dependencyScopes = mutableSetOf<Scope>()

    dependencies.forEach {
        if (it.scope != null) {
            if (!dependencyScopes.add(it.scope)) {
                error("Duplicated scope ${it.scope}")
            }
        }
    }

    check(scope == null || !dependencyScopes.contains(scope)) {
        "Duplicated scope $scope"
    }

    check(scope != null || dependencyScopes.isEmpty()) {
        "Must have a scope if a dependency has a scope"
    }

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

    val bindings = mutableMapOf<Key, Binding<*>>()
    val mapBindings = mutableMapOf<Key, MutableMap<Any?, Binding<*>>>()
    val setBindings = mutableMapOf<Key, MutableSet<Binding<*>>>()

    modules.forEach { module ->
        module.bindings.forEach { (key, binding) ->
            // todo override handling
            bindings[key] = binding
        }

        module.mapBindings.forEach { (mapKey, map) ->
            if (map.isNotEmpty()) {
                map.forEach { (entryKey, entryValueBinding) ->
                    val thisMap = mapBindings.getOrPut(mapKey) {
                        mutableMapOf()
                    }
                    // todo override handling
                    thisMap[entryKey] = entryValueBinding
                }
            } else {
                // ensure that the empty map exists
                mapBindings.getOrPut(mapKey) { mutableMapOf() }
            }
        }

        module.setBindings.forEach { (setKey, set) ->
            if (set.isNotEmpty()) {
                set.forEach { elementBinding ->
                    val thisSet = setBindings.getOrPut(setKey) { mutableSetOf() }
                    // todo override handling
                    thisSet.add(elementBinding)
                }
            } else {
                // ensure that the empty set exists
                setBindings.getOrPut(setKey) { mutableSetOf() }
            }
        }
    }

    return Component(scope, bindings, mapBindings, setBindings, dependencies)
}

internal fun Component.getAllBindingKeys(): Set<Key> =
    mutableSetOf<Key>().also { collectBindingKeys(it) }

internal fun Component.collectBindingKeys(
    keys: MutableSet<Key>
) {
    dependencies.forEach { it.collectBindingKeys(keys) }
    keys.addAll(this.bindings.keys)
}