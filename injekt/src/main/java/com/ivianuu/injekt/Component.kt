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
    internal val scopes: Iterable<KClass<out Annotation>>,
    internal val bindings: MutableMap<Key, Binding<*>>,
    internal val mapBindings: MapBindings?,
    internal val setBindings: SetBindings?,
    internal val dependencies: Iterable<Component>
) {

    private val linker = Linker(this)
    private val linkedBindings = hashMapOf<Key, LinkedBinding<*>>()

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T = getBinding<T>(keyOf(type, name)).get(parameters)

    internal fun <T> getBinding(key: Key): LinkedBinding<T> =
        findBinding(key, true)
            ?: error("Couldn't find a binding for $key")

    @Suppress("UNCHECKED_CAST")
    private fun <T> findBinding(key: Key, fullLookup: Boolean): LinkedBinding<T>? {
        if (key.type.parameters.size == 1) {
            when (key.type.raw) {
                Provider::class -> {
                    val realKey = keyOf(key.type.parameters.first(), key.name)
                    return LinkedProviderBinding<Any?>(this, realKey) as LinkedBinding<T>
                }
                Lazy::class -> {
                    val realKey = keyOf(key.type.parameters.first(), key.name)
                    return LinkedLazyBinding<Any?>(this, realKey) as LinkedBinding<T>
                }
            }
        }

        var binding: Binding<*>? = linkedBindings[key]
        if (binding != null) return binding as LinkedBinding<T>

        binding = bindings[key]
        if (binding != null) {
            binding = binding.performLink(linker)
            linkedBindings[key] = binding
            return binding as LinkedBinding<T>
        }

        for (dependency in dependencies) {
            binding = dependency.findBinding<T>(key, false)
            if (binding != null) return binding
        }

        if (fullLookup && key.name == null) {
            binding = JustInTimeBindings.find<T>(key)
            if (binding != null) {
                val scope = (binding as? HasScope)?.scope
                val component = findComponentForScope(scope)
                    ?: error("Couldn't find component for $scope")
                if (scope != null) {
                    binding = binding.asScoped()
                }
                return component.addBinding(key, binding) as LinkedBinding<T>
            }
        }

        return null
    }

    private fun <T> addBinding(key: Key, binding: Binding<T>): LinkedBinding<T> {
        bindings[key] = binding
        val linkedBinding = binding.performLink(linker)
        linkedBindings[key] = linkedBinding
        return linkedBinding
    }

    private fun findComponentForScope(scope: KClass<out Annotation>?): Component? {
        if (scope == null) return this
        if (scopes.contains(scope)) return this
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

inline fun <reified T> Component.inject(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(typeOf(), name, parameters)

fun <T> Component.inject(
    type: Type<T>,
    name: Any? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(type, name, parameters) }