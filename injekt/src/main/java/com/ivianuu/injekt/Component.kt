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
    val instances: MutableMap<Key, Instance<*>>,
    val dependencies: Iterable<Component>,
    val mapBindings: Map<Key, Map<Any?, Instance<*>>>,
    val setBindings: Map<Key, Set<Instance<*>>>
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
                findInstance<T>(key, true)
                    ?.let {
                        return@get lazy { it.get(parameters) } as T
                    }
            }
            Map::class -> {
                when (type.parameters[1].raw) {
                    Lazy::class -> {
                        val mapKeyType = type.parameters[0]
                        val mapValueType = type.parameters[1].parameters[0]
                        val mapKey = Key(
                            typeOf<Map<Any?, Any?>>(Map::class, mapKeyType, mapValueType)
                        )

                        mapBindings[mapKey]
                            ?.mapValues {
                                lazy { it.value.get(parameters) }
                            }
                            ?.let { return@get it as T }
                    }
                    Provider::class -> {
                        val mapKeyType = type.parameters[0]
                        val mapValueType = type.parameters[1].parameters[0]
                        val mapKey = Key(
                            typeOf<Map<Any?, Any?>>(Map::class, mapKeyType, mapValueType)
                        )

                        mapBindings[mapKey]
                            ?.mapValues { (_, instance) ->
                                provider { instance.get(it) }
                            }
                            ?.let { return@get it as T }
                    }
                    else -> {
                        val mapKeyType = type.parameters[0]
                        val mapValueType = type.parameters[1]
                        val mapKey = Key(
                            typeOf<Map<Any?, Any?>>(Map::class, mapKeyType, mapValueType)
                        )

                        mapBindings[mapKey]
                            ?.mapValues { it.value.get(parameters) }
                            ?.let { return@get it as T }
                    }
                }
            }
            Provider::class -> {
                val key = Key(type.parameters.first(), name)
                findInstance<T>(key, true)
                    ?.let { instance ->
                        return@get provider { instance.get(it ?: parameters) } as T
                    }
            }
            Set::class -> {
                when (type.parameters.first().raw) {
                    Lazy::class -> {
                        val setValueType = type.parameters[0].parameters[0]
                        val setKey = Key(
                            typeOf<Set<Any?>>(Set::class, setValueType)
                        )

                        setBindings[setKey]
                            ?.map { lazy { it.get(parameters) } }
                            ?.toSet()
                            ?.let { return@get it as T }
                    }
                    Provider::class -> {
                        val setValueType = type.parameters[0].parameters[0]
                        val setKey = Key(
                            typeOf<Set<Any?>>(Set::class, setValueType)
                        )

                        setBindings[setKey]
                            ?.map { instance ->
                                provider { instance.get(it ?: parameters) }
                            }
                            ?.toSet()
                            ?.let { return@get it as T }
                    }
                    else -> {
                        val setValueType = type.parameters[0]
                        val setKey = Key(
                            typeOf<Set<Any?>>(Set::class, setValueType)
                        )

                        setBindings[setKey]
                            ?.map { it.get(parameters) }
                            ?.toSet()
                            ?.let { return@get it as T }
                    }
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
 * Returns a new [Component] composed by all of [modules] and [dependencies]
 */
fun component(
    scope: Scope? = null,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component {
    val dependencyBindingsList = dependencies
        .flatMap { it.getAllBindings() }

    val dependencyBindings = mutableMapOf<Key, Binding<*>>()
    dependencyBindingsList.forEach { binding ->
        val oldBinding = dependencyBindings[binding.key]
        // todo better error message
        if (oldBinding != null && !binding.override) {
            error("Already declared binding for ${binding.key}")
        }

        dependencyBindings[binding.key] = binding
    }

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
        module.mapBindings.forEach { mapBindings.getOrPut(it) { mutableMapOf() } }
        module.setBindings.forEach { setBindings.getOrPut(it) { mutableSetOf() } }

        module.bindings.forEach { addBinding(it.value) }
        module.includes.forEach { addModule(it) }
    }

    modules.forEach { addModule(it) }

    return Component(scope, bindings, instances, dependencies, mapBindings, setBindings)
}

/**
 * Returns the instance matching [T] and [name]
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
): Lazy<T> = inject(typeOf(), name, parameters)

/**
 * Lazy version of [Component.get]
 */
fun <T> Component.inject(
    type: Type<T>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(type, name, parameters) }