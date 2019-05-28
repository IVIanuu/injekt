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
    val scope: Any?,
    val bindings: Map<Key, Binding<*>>,
    val instances: MutableMap<Key, Instance<*>>,
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
            .onEach { it.value.context = context }
            .forEach { it.value.attached() }
    }

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: KClass<*>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T {
        val key = Key(type, name)
        val instance = findInstance<T>(key, true)
            ?: throw IllegalStateException("Couldn't find a binding for $key")
        return instance.get(parameters)
    }

    /**
     * Returns the instance matching the [type] and [name] or null
     */
    fun <T> getOrNull(
        type: KClass<*>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T? {
        val key = Key(type, name)
        val instance = findInstance<T>(key, true)
        return instance?.get(parameters)
    }

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
    scope: Any? = null,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component {
    val dependencyBindings = dependencies
        .flatMap { it.getAllBindings() }
        .associateBy { it.key }

    val bindings = mutableMapOf<Key, Binding<*>>()
    val instances = mutableMapOf<Key, Instance<*>>()

    // todo clean up

    fun addBinding(binding: Binding<*>) {
        if (!binding.override &&
            (bindings.contains(binding.key)
                    || dependencyBindings.contains(binding.key))
        ) {
            error("Already declared binding for ${binding.key}")
        }

        val instance = binding.kind.createInstance(binding)

        bindings[binding.key] = binding
        instances[binding.key] = instance

        binding.additionalKeys.forEach {
            if (!binding.override &&
                (bindings.contains(it)
                        || dependencyBindings.contains(it))
            ) {
                error("Already declared binding for $it")
            }

            bindings[it] = binding
            instances[it] = instance
        }
    }

    fun addModule(module: Module) {
        module.bindings.forEach { addBinding(it.value) }
        module.includes.forEach { addModule(it) }
    }

    modules.forEach { addModule(it) }

    return Component(scope, bindings, instances, dependencies)
}

/**
 * Returns the instance matching the [type] and [name]
 */
inline fun <reified T> Component.get(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/**
 * Returns the instance matching the [type] and [name] or null
 */
inline fun <reified T> Component.getOrNull(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T? = getOrNull(T::class, name, parameters)

/**
 * Lazy version of [Component.get]
 */
inline fun <reified T> Component.inject(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get<T>(T::class, name, parameters) }

/**
 * Lazy version of [Component.getOrNull]
 */
inline fun <reified T> Component.injectOrNull(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T?> = lazy(LazyThreadSafetyMode.NONE) { getOrNull<T>(T::class, name, parameters) }