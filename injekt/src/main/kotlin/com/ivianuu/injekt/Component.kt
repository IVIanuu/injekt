/*
 * Copyright 2019 Manuel Wrage
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
    internal val allBindings: MutableMap<Key, Binding<*>>,
    internal val unlinkedUnscopedBindings: Map<Key, Binding<*>>,
    internal val mapBindings: MapBindings?,
    internal val setBindings: SetBindings?,
    internal val dependencies: Iterable<Component>
) {

    internal val linker = Linker(this)

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T = getBinding<T>(keyOf(type, name))(parameters)

    internal fun <T> get(key: Key, parameters: ParametersDefinition? = null): T =
        getBinding<T>(key)()

    internal fun <T> getBinding(key: Key): LinkedBinding<T> =
        findBinding(key) ?: error("Couldn't find a binding for $key")

    @Suppress("UNCHECKED_CAST")
    private fun <T> findBinding(key: Key): LinkedBinding<T>? {
        var binding: Binding<T>?

        // providers, lazy
        binding = findSpecialBinding(key)
        if (binding != null) return binding

        binding = findExplicitBinding(key)
        if (binding != null) return binding

        binding = findUnscopedBinding(key)
        if (binding != null) return binding

        binding = findJustInTimeBinding(key)
        if (binding != null) return binding

        return null
    }

    private fun <T> findSpecialBinding(key: Key): LinkedBinding<T>? {
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

        return null
    }

    private fun <T> findExplicitBinding(key: Key): LinkedBinding<T>? {
        var binding = synchronized(allBindings) { allBindings[key] } as? Binding<T>
        if (binding != null) return binding.linkIfNeeded(key)

        for (dependency in dependencies) {
            binding = dependency.findExplicitBindingForDependency(key)
            if (binding != null) return binding
        }

        return null
    }

    private fun <T> findExplicitBindingForDependency(key: Key): LinkedBinding<T>? {
        var binding = synchronized(allBindings) { allBindings[key] } as? Binding<T>
        if (binding != null && !binding.unscoped) return binding.linkIfNeeded(key)

        for (dependency in dependencies) {
            binding = dependency.findExplicitBindingForDependency(key)
            if (binding != null) return binding
        }

        return null
    }

    private fun <T> findUnscopedBinding(key: Key): LinkedBinding<T>? {
        for (dependency in dependencies) {
            val binding = dependency.findUnscopedBindingForDependency<T>(key)
            if (binding != null) return addJitBinding(key, binding)
        }

        return null
    }

    private fun <T> findUnscopedBindingForDependency(key: Key): Binding<T>? {
        var binding = unlinkedUnscopedBindings[key] as? Binding<T>
        if (binding != null) return binding

        for (dependency in dependencies) {
            binding = dependency.findUnscopedBindingForDependency(key)
            if (binding != null) return binding
        }

        return null
    }

    private fun <T> findJustInTimeBinding(key: Key): LinkedBinding<T>? {
        val jitLookup = InjektPlugins.justInTimeLookupFactory.create<T>(key)
        if (jitLookup != null) {
            var binding = jitLookup.binding
            return if (jitLookup.scope != null) {
                val component = findComponentForScope(jitLookup.scope)
                    ?: error("Couldn't find component for ${jitLookup.scope}")
                binding = binding.asScoped()
                component.addJitBinding(key, binding)
            } else {
                binding.unscoped = true
                addJitBinding(key, binding)
            }
        }

        return null
    }

    private fun <T> addJitBinding(
        key: Key,
        binding: Binding<T>
    ): LinkedBinding<T> {
        val linkedBinding = binding.performLink(linker)
        synchronized(allBindings) { allBindings[key] = linkedBinding }
        return linkedBinding
    }

    private fun <T> Binding<T>.linkIfNeeded(key: Key): LinkedBinding<T> {
        if (this is LinkedBinding) return this
        val linkedBinding = performLink(linker)
        synchronized(allBindings) { allBindings[key] = linkedBinding }
        return linkedBinding
    }

    private fun findComponentForScope(scope: KClass<out Annotation>): Component? {
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
): kotlin.Lazy<T> = inject(typeOf(), name, parameters)

fun <T> Component.inject(
    type: Type<T>,
    name: Any? = null,
    parameters: ParametersDefinition? = null
): kotlin.Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(type, name, parameters) }