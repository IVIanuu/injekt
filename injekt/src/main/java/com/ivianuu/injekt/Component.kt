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
 */
class Component internal constructor(
    val scope: Scope?,
    val bindings: Map<Key, Binding<*>>,
    val instances: Map<Key, Instance<*>>,
    val dependencies: Iterable<Component>,
    internal val mapBindings: Map<Key, Map<Any?, Instance<*>>>,
    internal val setBindings: Map<Key, Set<Instance<*>>>
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
        when (type.raw) {
            Lazy::class -> {
                val key = Key(type.parameters.first(), name)
                val instance = findInstance<T>(key)
                    ?: throw IllegalStateException("Couldn't find a binding for $key")
                return lazy { instance.get(parameters) } as T
            }
            Map::class -> {
                when (type.parameters[1].raw) {
                    Lazy::class -> {
                        val mapKeyType = type.parameters[0]
                        val mapValueType = type.parameters[1].parameters[0]
                        val mapKey = Key(
                            typeOf<Map<Any?, Any?>>(Map::class, mapKeyType, mapValueType)
                        )

                        val map = mapBindings[mapKey]
                            ?: return emptyMap<Any?, Any?>() as T

                        return map.mapValues {
                            lazy { it.value.get(parameters) }
                        } as T
                    }
                    Provider::class -> {
                        val mapKeyType = type.parameters[0]
                        val mapValueType = type.parameters[1].parameters[0]
                        val mapKey = Key(
                            typeOf<Map<Any?, Any?>>(Map::class, mapKeyType, mapValueType)
                        )

                        val map = mapBindings[mapKey]
                            ?: return emptyMap<Any?, Any?>() as T

                        return map.mapValues { (_, instance) ->
                            provider { instance.get(it) }
                        } as T
                    }
                    else -> {
                        val mapKeyType = type.parameters[0]
                        val mapValueType = type.parameters[1]
                        val mapKey = Key(
                            typeOf<Map<Any?, Any?>>(Map::class, mapKeyType, mapValueType)
                        )

                        val map = mapBindings[mapKey]
                            ?: return emptyMap<Any?, Any?>() as T

                        return map.mapValues { it.value.get(parameters) } as T
                    }
                }
            }
            Provider::class -> {
                val key = Key(type.parameters.first(), name)
                val instance = findInstance<T>(key)
                    ?: throw IllegalStateException("Couldn't find a binding for $key")
                return provider { instance.get(it ?: parameters) } as T
            }
            Set::class -> {
                when (type.parameters.first().raw) {
                    Lazy::class -> {
                        val setValueType = type.parameters[0].parameters[0]
                        val setKey = Key(
                            typeOf<Set<Any?>>(Set::class, setValueType)
                        )

                        val set = setBindings[setKey]
                            ?: return emptySet<Any?>() as T

                        return set.map {
                            lazy { it.get(parameters) }
                        }.toSet() as T
                    }
                    Provider::class -> {
                        val setValueType = type.parameters[0].parameters[0]
                        val setKey = Key(
                            typeOf<Set<Any?>>(Set::class, setValueType)
                        )

                        val set = setBindings[setKey]
                            ?: return emptySet<Any?>() as T

                        return set.map { instance ->
                            provider { instance.get(it ?: parameters) }
                        }.toSet() as T
                    }
                    else -> {
                        val setValueType = type.parameters[0]
                        val setKey = Key(
                            typeOf<Set<Any?>>(Set::class, setValueType)
                        )

                        val set = setBindings[setKey]
                            ?: return emptySet<Any?>() as T

                        return set.map { it.get(parameters) }.toSet() as T
                    }
                }
            }
            else -> {
                val key = Key(type, name)
                val instance = findInstance<T>(key)
                    ?: throw IllegalStateException("Couldn't find a binding for $key")
                return instance.get(parameters)
            }
        }
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
    val mapBindings =
        mutableMapOf<Key, MutableMap<Any?, Instance<*>>>()
    val setBindings =
        mutableMapOf<Key, MutableSet<Instance<*>>>()

    dependencies.forEach {
        mapBindings.putAll(it.mapBindings as Map<out Key, MutableMap<Any?, Instance<*>>>)
        setBindings.putAll(it.setBindings as Map<out Key, MutableSet<Instance<*>>>)
    }

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
            else error("Already declared binding for $key")
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

        binding.mapBindings.forEach { (key, mapBinding) ->
            val map = mapBindings.getOrPut(key) { mutableMapOf() }
            // todo check overrides
            map[mapBinding.key] = instance
        }

        binding.setBindings.forEach { (key, setBinding) ->
            val set = setBindings.getOrPut(key) { mutableSetOf() }
            // todo check overrides
            set.add(instance)
        }
    }

    fun addModule(module: Module) {
        module.bindings.forEach { addBinding(it.value, false) }
        module.includes.forEach { addModule(it) }
    }

    modules.forEach { addModule(it) }

    Component.bindingsByScope[scope]?.forEach { addBinding(it, true) }
    instances.putAll(Component.unscopedInstances)

    return Component(scope, bindings, instances, dependencies, mapBindings, setBindings)
}

/**
 * Returns the instance matching the [type] and [name]
 */
inline fun <reified T> Component.get(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(typeOf(), name, parameters)

/**
 * Lazy version of [Component.get]
 */
inline fun <reified T> Component.inject(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(typeOf<T>(), name, parameters)

/**
 * Lazy version of [Component.get]
 */
fun <T> Component.inject(
    type: Type<T>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get<T>(type, name, parameters) }