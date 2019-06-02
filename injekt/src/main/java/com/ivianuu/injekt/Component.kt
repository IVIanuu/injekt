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

import kotlin.reflect.KClass

/**
 * The actual dependency container which provides bindings
 */
class Component internal constructor(
    val scope: Scope?,
    val bindings: Map<Key, Binding<*>>,
    val instances: Map<Key, Instance<*>>,
    val dependencies: Iterable<Component>
) {

    /**
     * The definition context of this component
     */
    val context = DefinitionContext(this)

    init {
        InjektPlugins.logger?.let { logger ->
            instances.forEach {
                logger.info("Register binding ${it.value.binding}")
            }
        }
        instances
            .onEach { it.value.attachedContext = context }
            .forEach { it.value.attached() }
    }

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: KClass<*>,
        name: Qualifier? = null,
        parameters: ParametersDefinition? = null
    ): T {
        val key = Key(type, name)
        val instance = findInstance<T>(key)
            ?: throw IllegalStateException("Couldn't find a binding for $key")
        return instance.get(context, parameters)
    }

    /**
     * Returns the instance matching the [type] and [name] or null
     */
    fun <T> getOrNull(
        type: KClass<*>,
        name: Qualifier? = null,
        parameters: ParametersDefinition? = null
    ): T? {
        val key = Key(type, name)
        val instance = findInstance<T>(key)
        return instance?.get(context, parameters)
    }

    private fun <T> findInstance(key: Key): Instance<T>? {
        var instance = instances[key]
        if (instance != null) return instance as Instance<T>

        for (dependency in dependencies) {
            instance = dependency.findInstance<T>(key)
            if (instance != null) return instance
        }

        return null
    }

    companion object {
        internal val bindingsByScope = mutableMapOf<Scope, MutableSet<Binding<*>>>()

        internal val unscopedBindings = mutableSetOf<Binding<*>>()
        internal val unscopedInstances = mutableMapOf<Key, Instance<*>>()

        init {
            FastServiceLoader.load(MultiCreator::class.java, javaClass.classLoader)
                .flatMap { it.create() }
                .forEach { binding ->
                    if (binding.scope != null) {
                        bindingsByScope.getOrPut(binding.scope) { mutableSetOf() }
                            .add(binding)
                    } else {
                        unscopedBindings.add(binding)
                        val instance = binding.kind.createInstance(binding)
                        // todo check overrides
                        unscopedInstances[binding.key] = instance
                        binding.additionalKeys.forEach {
                            unscopedInstances[it] = instance
                        }
                    }
                }
        }
    }
}

/**
 * Returns a new [Component] composed by all of [modules] and [dependencies]
 */
fun component(
    scope: Scope? = null,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component {
    val dependencyBindings = dependencies
        .flatMap { it.getAllBindings() }
        .associateBy { it.key }

    val bindings = mutableMapOf<Key, Binding<*>>()
    val instances = mutableMapOf<Key, Instance<*>>()

    // todo clean up

    fun addBindingByKey(
        key: Key,
        binding: Binding<*>,
        instance: Instance<*>,
        dropOverride: Boolean
    ) {
        if (!binding.override &&
            (bindings.contains(key)
                    || dependencyBindings.contains(key))
        ) {
            if (dropOverride) return
            else error("Already declared binding for ${key}")
        }

        bindings[key] = binding
        instances[key] = instance
    }

    fun addBinding(binding: Binding<*>, dropOverride: Boolean) {
        val instance = binding.kind.createInstance(binding)
        addBindingByKey(binding.key, binding, instance, dropOverride)
        binding.additionalKeys.forEach {
            addBindingByKey(it, binding, instance, dropOverride)
        }
    }

    fun addModule(module: Module) {
        module.bindings.forEach { addBinding(it.value, false) }
        module.includes.forEach { addModule(it) }
    }

    modules.forEach { addModule(it) }

    Component.bindingsByScope[scope]?.forEach { addBinding(it, true) }
    instances.putAll(Component.unscopedInstances)

    return Component(scope, bindings, instances, dependencies)
}

/**
 * Returns the instance matching the [type] and [name]
 */
inline fun <reified T> Component.get(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/**
 * Returns the instance matching the [type] and [name] or null
 */
inline fun <reified T> Component.getOrNull(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T? = getOrNull(T::class, name, parameters)

/**
 * Lazy version of [Component.get]
 */
inline fun <reified T> Component.inject(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/**
 * Lazy version of [Component.get]
 */
fun <T> Component.inject(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get<T>(type, name, parameters) }

/**
 * Lazy version of [Component.getOrNull]
 */
inline fun <reified T> Component.injectOrNull(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T?> = injectOrNull(T::class, name, parameters)

/**
 * Lazy version of [Component.getOrNull]
 */
fun <T> Component.injectOrNull(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T?> = lazy(LazyThreadSafetyMode.NONE) { getOrNull<T>(type, name, parameters) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.getProvider(
    name: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = getProvider(T::class, name, defaultParameters)

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
fun <T> Component.getProvider(
    type: KClass<*>,
    name: Qualifier? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = provider { parameters: ParametersDefinition? ->
    get<T>(type, name, parameters ?: defaultParameters)
}

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.injectProvider(
    name: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = injectProvider(T::class, name, defaultParameters)

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
fun <T> Component.injectProvider(
    type: KClass<*>,
    name: Qualifier? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazy(LazyThreadSafetyMode.NONE) {
    provider { parameters: ParametersDefinition? ->
        get<T>(type, name, parameters ?: defaultParameters)
    }
}

/**
 * Returns a multi bound [Map] for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.getMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Map<K, V> {
    return getMultiBindingMap(name).mapValues {
        get<V>(it.value.type, it.value.name, parameters)
    }
}

/**
 * Returns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.getLazyMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<V>> {
    return getMultiBindingMap(name).mapValues {
        lazy { get<V>(it.value.type, it.value.name, parameters) }
    }
}

/**
 * Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <K, V> Component.getProviderMap(
    name: MapName<K, V>,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<V>> {
    return getMultiBindingMap(name).mapValues { (_, binding) ->
        provider {
            get<V>(
                binding.type,
                binding.name,
                it ?: defaultParameters
            )
        }
    }
}

/**
 * Lazily returns a multi bound [Map] for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.injectMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, V>> {
    return lazy { getMap(name, parameters) }
}

/**
 * Lazily returns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.injectLazyMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<V>>> =
    lazy { getLazyMap(name, parameters) }

/**
 * Lazily returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <K, V> Component.injectProviderMap(
    name: MapName<K, V>,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<V>>> =
    lazy { getProviderMap(name, defaultParameters) }

/**
 * Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.getSet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Set<T> {
    return getMultiBindingSet(name)
        .map { get<T>(it.type, it.name, parameters) }
        .toSet()
}

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.getLazySet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> {
    return getMultiBindingSet(name).map {
        lazy { get<T>(it.type, it.name, parameters) }
    }.toSet()
}

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.getProviderSet(
    name: SetName<T>,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> {
    return getMultiBindingSet(name).map { binding ->
        provider {
            get<T>(
                binding.type,
                binding.name,
                it ?: defaultParameters
            )
        }
    }.toSet()
}

/**
 * Lazily returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.injectSet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> = lazy(LazyThreadSafetyMode.NONE) { getSet(name, parameters) }

/**
 * Lazily returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.injectLazySet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { getLazySet(name, parameters) }

/**
 * Lazily returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.injectProviderSet(
    name: SetName<T>,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { getProviderSet(name, defaultParameters) }