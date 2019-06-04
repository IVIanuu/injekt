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

/**
 * The actual dependency container which provides bindings
 * Dependencies can be requested by calling either [get] or [inject]
 */
class Component internal constructor(
    val scope: Scope?,
    internal val instances: MutableMap<Key, Instance<*>>,
    internal val dependencies: Iterable<Component>,
    internal val mapBindings: Map<Key, Map<Any?, Instance<*>>>,
    internal val setBindings: Map<Key, Set<Instance<*>>>
) {

    private val context = DefinitionContext(this)

    init {
        InjektPlugins.logger?.let { logger ->
            logger.info("${scopeName()} initialize")

            dependencies.forEach {
                logger.info("${scopeName()} Register dependency ${it.scopeName()}")
            }

            instances.forEach {
                logger.info("${scopeName()} Register binding ${it.value.binding}")
            }

            mapBindings.forEach { (key, map) ->
                logger.info("${scopeName()} Register map binding $key ${map.mapValues { it.value.binding }}")
            }

            setBindings.forEach { (key, set) ->
                logger.info("${scopeName()} Register set binding $key ${set.map { it.binding }}")
            }
        }
        instances
            .onEach { it.value.context = context }
            .forEach { it.value.attached() }
    }

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: Type<T>,
        name: Qualifier? = null,
        parameters: ParametersDefinition? = null
    ): T {
        // todo add those at component init?
        when (type.raw) {
            Lazy::class -> {
                val key = Key(type.parameters.first(), name)
                findInstance<T>(key, true)
                    ?.let {
                        return@get lazy(LazyThreadSafetyMode.NONE) { it.get(parameters) } as T
                    }
            }
            Provider::class -> {
                val key = Key(type.parameters.first(), name)
                findInstance<T>(key, true)
                    ?.let { instance ->
                        return@get provider { instance.get(it ?: parameters) } as T
                    }
            }
            else -> {
                val key = Key(type, name)
                findInstance<T>(key, true)
                    ?.let { return@get it.get(parameters) }
            }
        }

        // todo clean up
        throw IllegalStateException("Couldn't find a binding for ${Key(type, name)}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findInstance(key: Key, includeGlobalPool: Boolean): Instance<T>? {
        var instance = instances[key]
        if (instance != null) return instance as Instance<T>

        for (dependency in dependencies) {
            instance = dependency.findInstance<T>(key, false)
            if (instance != null) return instance
        }

        if (includeGlobalPool) {
            val binding = GlobalPool.get<T>(key)
            if (binding != null) {
                val component = findComponentForScope(scope)
                    ?: error("Couldn't find component for $scope")
                return component.addInstance(binding)
            }
        }

        return null
    }

    private fun <T> addInstance(binding: Binding<T>): Instance<T> {
        val instance = binding.kind.createInstance(binding)
        instances[binding.key] = instance
        instance.context = context
        instance.attached()
        return instance
    }

    private fun findComponentForScope(scope: Any?): Component? {
        if (scope == null) return this
        if (this.scope == scope) return this
        for (dependency in dependencies) {
            dependency.findComponentForScope(scope)
                ?.let { return@findComponentForScope it }
        }

        return null
    }

}

/**
 * Constructs a new [Component] which will be driven by all of [dependencies] and [modules]
 */
fun component(
    scope: Scope? = null,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component {
    // todo clean up the whole function

    val dependencyBindings = dependencies
        .map { it.getAllBindings() }
        .fold(mutableMapOf<Key, Binding<*>>()) { acc, current ->
            current.forEach { (key, binding) ->
                val oldBinding = acc[key]
                // todo better error message
                if (oldBinding != null && !binding.override) {
                    error("Already declared binding for $key")
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
    ).apply {
        if (scope != null) bindName(scope)
    }
    addBinding(componentBinding)

    // maps
    mapBindings.forEach { (key, map) ->
        val keyType = key.type.parameters[0]
        val valueType = key.type.parameters[1]
        val instanceBinding = binding(
            type = typeOf(Map::class, keyType, valueType),
            kind = FactoryKind,
            override = true,
            definition = { map.mapValues { it.value.get() } }
        )

        addBinding(instanceBinding)

        val lazyBinding = binding(
            type = typeOf(Map::class, keyType, typeOf<Any?>(Lazy::class, valueType)),
            kind = FactoryKind,
            override = true,
            definition = {
                map.mapValues { lazy(LazyThreadSafetyMode.NONE) { it.value.get() } }
            }
        )

        addBinding(lazyBinding)

        val providerBinding = binding(
            type = typeOf(Map::class, keyType, typeOf<Any?>(Provider::class, valueType)),
            kind = FactoryKind,
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
            type = typeOf(Set::class, valueType),
            kind = FactoryKind,
            override = true,
            definition = {
                map.values
                    .map { it.get() }
                    .toSet()
            }
        )

        addBinding(instanceBinding)

        val lazyBinding = binding(
            type = typeOf(Set::class, typeOf<Any?>(Lazy::class, valueType)),
            kind = FactoryKind,
            override = true,
            definition = {
                map.values
                    .map { lazy(LazyThreadSafetyMode.NONE) { it.get() } }
                    .toSet()
            }
        )

        addBinding(lazyBinding)

        val providerBinding = binding(
            type = typeOf(Set::class, typeOf<Any?>(Provider::class, valueType)),
            kind = FactoryKind,
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

inline fun <reified T> Component.get(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(typeOf(), name, parameters)

inline fun <reified T> Component.inject(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(typeOf(), name, parameters)

fun <T> Component.inject(
    type: Type<T>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(type, name, parameters) }

fun Component.scopeName() = scope?.toString() ?: "Unscoped"
