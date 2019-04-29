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
class Component internal constructor() {

    /**
     * All dependencies of this component
     */
    val dependencies: Set<Component> get() = _dependencies
    private val _dependencies = linkedSetOf<Component>()

    /**
     * All bindings of this component
     */
    val bindings: Collection<Binding<*>> get() = _instances.values.map { it.binding }

    /**
     * All instances of this component
     */
    val instances: Collection<Instance<*>> get() = _instances.values
    private val _instances = linkedMapOf<Key, Instance<*>>()

    /**
     * The definition context of this component
     */
    val context = DefinitionContext(this)

    /**
     * Returns a instance of [T] matching the [type], [name] and [parameters]
     */
    fun <T> get(
        type: KClass<*>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T {
        val key = Key(type, name)

        val instance = findInstance<T>(key)
            ?: throw IllegalStateException("Couldn't find a binding for $key")

        return instance.get(context, parameters)
    }

    /**
     * Adds the [dependency] as a dependency
     */
    fun addDependency(dependency: Component) {
        _dependencies.add(dependency)
    }

    /**
     * Adds all binding of the [module]
     */
    fun addModule(module: Module) {
        module.bindings.forEach { addBinding(it) }
        module.includes.forEach { addModule(it) }
    }

    /**
     * Saves the [binding]
     */
    fun addBinding(binding: Binding<*>) {
        _instances[binding.key] = binding.kind.createInstance(binding)
        binding.additionalBindings.forEach { addBinding(it) }
    }

    private fun <T> findInstance(key: Key): Instance<T>? {
        var instance = _instances[key]
        if (instance != null) return instance as Instance<T>

        for (dependency in _dependencies) {
            instance = dependency.findInstance<T>(key)
            if (instance != null) return instance
        }

        return null
    }

}

/**
 * Returns a new [Component] configured by [block]
 */
fun component(
    block: (Component.() -> Unit)? = null
): Component {
    return Component().apply { block?.invoke(this) }
}

/**
 * Returns a instance of [T] matching the [name] and [parameters]
 */
inline fun <reified T> Component.get(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/**
 * Lazily returns a instance of [T] matching the [name] and [parameters]
 */
inline fun <reified T> Component.inject(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/**
 * Lazily returns a instance of [T] matching the [name] and [parameters]
 */
fun <T> Component.inject(
    type: KClass<*>,
    name: Any? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get<T>(type, name, parameters) }

/**
 * Adds all [dependencies]
 */
fun Component.dependencies(dependencies: Iterable<Component>) {
    dependencies.forEach { addDependency(it) }
}

/**
 * Adds all [dependencies]
 */
fun Component.dependencies(vararg dependencies: Component) {
    dependencies.forEach { addDependency(it) }
}

/**
 * Adds all [modules]
 */
fun Component.modules(modules: Iterable<Module>) {
    modules.forEach { addModule(it) }
}

/**
 * Adds all [modules]
 */
fun Component.modules(vararg modules: Module) {
    modules.forEach { addModule(it) }
}