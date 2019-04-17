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
    /**
     * All dependencies of this component
     */
    val dependencies: List<Component>,
    /**
     * All bindings of this component
     */
    val bindings: Map<Key, Binding<*>>,
    private val instances: Map<Key, Instance<*>>
) {

    /**
     * The definition context of this component
     */
    val context = DefinitionContext(this)

    init {
        // set context for each instance
        instances.forEach { it.value.setDefinitionContext(context) }
    }

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

        return instance.get(parameters)
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

}

/**
 * Returns a new [Component] configured by [block]
 */
fun component(
    block: (ComponentBuilder.() -> Unit)? = null
): Component {
    return ComponentBuilder()
        .apply { block?.invoke(this) }
        .build()
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
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.getProvider(
    name: Any? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = getProvider(T::class, name, defaultParameters)

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
fun <T> Component.getProvider(
    type: KClass<*>,
    name: Any? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = provider { parameters: ParametersDefinition? ->
    get<T>(type, name, parameters ?: defaultParameters)
}

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.injectProvider(
    name: Any? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = injectProvider(T::class, name, defaultParameters)

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
fun <T> Component.injectProvider(
    type: KClass<*>,
    name: Any? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazy(LazyThreadSafetyMode.NONE) {
    provider { parameters: ParametersDefinition? ->
        get<T>(type, name, parameters ?: defaultParameters)
    }
}