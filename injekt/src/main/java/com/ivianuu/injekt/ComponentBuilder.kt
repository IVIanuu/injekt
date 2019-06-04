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

    val dependencyBindings = dependencies
        .map { it.getAllBindings() }
        .fold(mutableMapOf<Key, Binding<*>>()) { acc, current ->
            current.forEach { (key, binding) ->
                val oldBinding = acc[key]
                check(oldBinding == null || binding.override) {
                    "Already declared binding for $key"
                }
                acc[key] = binding
            }

            return@fold acc
        }

    val bindings = mutableMapOf<Key, Binding<*>>()
    val instances = mutableMapOf<Key, Instance<*>>()
    val mapBindings =
        mutableMapOf<Key, MutableMap<Any?, Instance<*>>>()
    val setBindings =
        mutableMapOf<Key, MutableMap<Key, Instance<*>>>()

    dependencies.forEach { dependency ->
        mapBindings.putAll(dependency.mapBindings as Map<out Key, MutableMap<Any?, Instance<*>>>)
        setBindings.putAll(
            dependency.setBindings
                .mapValues { (_, set) ->
                    set.associateBy { it.binding.key }
                } as Map<out Key, MutableMap<Key, Instance<*>>>
        )
    }

    fun addBindingByKey(
        key: Key,
        binding: Binding<*>,
        instance: Instance<*>
    ) {
        if (!binding.override &&
            (bindings.contains(key)
                    || dependencyBindings.contains(key))
        ) {
            error("Already declared binding for $key")
        }

        bindings[key] = binding
        instances[key] = instance
    }

    fun addBinding(binding: Binding<*>) {
        val instance = binding.kind.createInstance(binding)
        addBindingByKey(binding.key, binding, instance)
        binding.additionalKeys.forEach {
            addBindingByKey(it, binding, instance)
        }

        binding.mapBindings.forEach { (mapKey, mapBinding) ->
            val map = mapBindings.getOrPut(mapKey) { mutableMapOf() }

            val oldValue = map[mapBinding.mapKey]
            if (oldValue != null && !mapBinding.override) {
                error("Already contains ${mapBinding.mapKey}")
            }

            map[mapBinding.key] = instance
        }

        binding.setBindings.forEach { (setKey, setBinding) ->
            val map = setBindings.getOrPut(setKey) { mutableMapOf() }

            val oldValue = map[binding.key]

            if (oldValue != null && !setBinding.override) {
                error("Already contains ${binding.key}")
            }

            map[binding.key] = instance
        }
    }

    fun addModule(module: Module) {
        module.mapBindings.forEach { mapBindings.getOrPut(it) { mutableMapOf() } }
        module.setBindings.forEach { setBindings.getOrPut(it) { mutableMapOf() } }

        module.bindings.forEach { addBinding(it.value) }
        module.includes.forEach { addModule(it) }
    }

    modules.forEach { addModule(it) }

    // component binding
    val componentBinding = binding(
        kind = SingleKind,
        override = true,
        definition = { this.component }
    ).apply { if (scope != null) bindName(scope) }
    addBinding(componentBinding)

    // maps
    mapBindings.forEach { (key, map) ->
        val keyType = key.type.parameters[0]
        val valueType = key.type.parameters[1]
        val instanceBinding = binding(
            kind = FactoryKind,
            type = typeOf(Map::class, keyType, valueType),
            name = key.name,
            override = true,
            definition = { map.mapValues { it.value.get() } }
        )

        addBinding(instanceBinding)

        val lazyBinding = binding(
            kind = FactoryKind,
            type = typeOf(Map::class, keyType, typeOf<Any?>(Lazy::class, valueType)),
            name = key.name,
            override = true,
            definition = {
                map.mapValues { lazy(LazyThreadSafetyMode.NONE) { it.value.get() } }
            }
        )

        addBinding(lazyBinding)

        val providerBinding = binding(
            kind = FactoryKind,
            type = typeOf(Map::class, keyType, typeOf<Any?>(Provider::class, valueType)),
            name = key.name,
            override = true,
            definition = {
                map.mapValues { entry ->
                    provider { entry.value.get(it) }
                }
            }
        )

        addBinding(providerBinding)
    }

    // sets
    setBindings.forEach { (key, map) ->
        val valueType = key.type.parameters[0]
        val instanceBinding = binding(
            kind = FactoryKind,
            type = typeOf(Set::class, valueType),
            name = key.name,
            override = true,
            definition = {
                map.values
                    .map { it.get() }
                    .toSet()
            }
        )

        addBinding(instanceBinding)

        val lazyBinding = binding(
            kind = FactoryKind,
            type = typeOf(Set::class, typeOf<Any?>(Lazy::class, valueType)),
            name = key.name,
            override = true,
            definition = {
                map.values
                    .map { lazy(LazyThreadSafetyMode.NONE) { it.get() } }
                    .toSet()
            }
        )

        addBinding(lazyBinding)

        val providerBinding = binding(
            kind = FactoryKind,
            type = typeOf(Set::class, typeOf<Any?>(Provider::class, valueType)),
            name = key.name,
            override = true,
            definition = {
                map.values
                    .map { instance ->
                        provider { instance.get(it) }
                    }
                    .toSet()
            }
        )

        addBinding(providerBinding)
    }

    return Component(
        scope, instances, dependencies, mapBindings,
        setBindings.mapValues { it.value.values.toSet() }
    )
}

internal fun Component.getAllBindings(): Map<Key, Binding<*>> =
    mutableMapOf<Key, Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableMap<Key, Binding<*>>
) {
    dependencies.forEach { it.collectBindings(bindings) }
    bindings.putAll(this.instances.mapValues { it.value.binding })
}