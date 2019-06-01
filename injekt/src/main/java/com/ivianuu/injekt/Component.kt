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
        val instance = findInstance<T>(key, true)
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
        val instance = findInstance<T>(key, true)
        return instance?.get(context, parameters)
    }

    private fun <T> findInstance(key: Key, includeUnscoped: Boolean): Instance<T>? {
        var instance = instances[key]
        if (instance != null) return instance as Instance<T>

        for (dependency in dependencies) {
            instance = dependency.findInstance<T>(key, false)
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