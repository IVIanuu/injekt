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
                        return@get provider { instance.get(it) } as T
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

        if (includeGlobalPool && key.name == null) {
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
