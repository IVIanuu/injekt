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
 * Dependencies can be requested by calling either [get] or [inject]
 */
class Component internal constructor(
    val scope: KClass<out Annotation>?,
    internal val bindings: MutableMap<Key, BindingContribution<*>>,
    internal val mapBindings: MapBindings?,
    internal val setBindings: SetBindings?,
    internal val dependencies: Iterable<Component>
) {

    init {
        bindings.forEach { it.value.binding.attach(this) }
    }

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T = getBinding(type, name).get(parameters)

    /**
     * Returns the [Binding] matching [type] and [name]
     */
    fun <T> getBinding(type: Type<T>, name: Any? = null): Binding<T> =
        getBinding(keyOf(type, name))

    fun <T> getBinding(key: Key): Binding<T> =
        findBinding(key, true)
            ?: error("Couldn't find a binding for $key")

    @Suppress("UNCHECKED_CAST")
    private fun <T> findBinding(key: Key, fullLookup: Boolean): Binding<T>? {
        var binding = bindings[key]?.binding
        if (binding != null) return binding as Binding<T>

        for (dependency in dependencies) {
            binding = dependency.findBinding<T>(key, false)
            if (binding != null) return binding
        }

        if (fullLookup && key.type.parameters.size == 1) {
            when (key.type.raw) {
                Provider::class -> {
                    val realKey = keyOf(key.type.parameters.first(), key.name)
                    binding = ProviderBinding<T>(realKey)
                    addBinding(key, binding)
                    return binding as Binding<T>
                }
                Lazy::class -> {
                    val realKey = keyOf(key.type.parameters.first(), key.name)
                    binding = LazyBinding<T>(realKey)
                    addBinding(key, binding)
                    return binding as Binding<T>
                }
            }
        }

        if (fullLookup && key.name == null) {
            val bindingFactory = JustInTimeBindings.find<T>(key)
            if (bindingFactory != null) {
                val component = findComponentForScope(bindingFactory.scope)
                    ?: error("Couldn't find component for $scope")
                binding = bindingFactory.create()
                component.addBinding(key, binding)
                return binding
            }
        }

        return null
    }

    private fun addBinding(key: Key, binding: Binding<*>) {
        binding.attach(this)
        bindings[key] = BindingContribution(binding, key, false)
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
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(typeOf(), name, parameters)

inline fun <reified T> Component.getBinding(name: Any? = null): Binding<T> =
    getBinding(typeOf(), name)

inline fun <reified T> Component.inject(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(typeOf(), name, parameters)

fun <T> Component.inject(
    type: Type<T>,
    name: Any? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(type, name, parameters) }