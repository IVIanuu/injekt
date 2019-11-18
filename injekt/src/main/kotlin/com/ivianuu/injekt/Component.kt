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
 * The heart of the library which provides instances
 * Dependencies can be requested by calling either [get] or [inject]
 * Use [ComponentBuilder] to construct component instances
 *
 * @see ComponentBuilder
 */
class Component internal constructor(
    internal val scopes: List<KClass<out Annotation>>,
    internal val allBindings: MutableMap<Key, Binding<*>>,
    internal val unlinkedUnscopedBindings: Map<Key, Binding<*>>,
    eagerBindings: List<Key>,
    internal val mapBindings: MapBindings,
    internal val setBindings: SetBindings,
    internal val dependencies: List<Component>
) {

    internal val linker = Linker(this)

    init {
        eagerBindings.forEach { get(it) }
    }

    inline fun <reified T> get(
        name: Any? = null,
        noinline parameters: ParametersDefinition? = null
    ): T = get(type = typeOf(), name = name, parameters = parameters)


    fun <T> get(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T = get(key = keyOf(type, name), parameters = parameters)

    /**
     * Retrieve a instance of [T]
     *
     * @param key the of the instance
     * @param parameters optional parameters to construct the instance
     * @return the instance
     */
    fun <T> get(key: Key, parameters: ParametersDefinition? = null): T =
        getBinding<T>(key)(parameters)

    inline fun <reified T> inject(
        name: Any? = null,
        noinline parameters: ParametersDefinition? = null
    ): kotlin.Lazy<T> = inject(type = typeOf(), name = name, parameters = parameters)

    /**
     * Lazy version of [get]
     *
     * @param type the type of key of the instance
     * @param name the name of the of the instance
     * @param parameters optional parameters to construct the instance
     * @return the instance

     * @see Component.get
     */
    fun <T> inject(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): kotlin.Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
        get(type = type, name = name, parameters = parameters)
    }

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
                    val instanceKey = keyOf(key.type.parameters.first(), key.name)
                    return LinkedProviderBinding<Any?>(this, instanceKey) as LinkedBinding<T>
                }
                Lazy::class -> {
                    val instanceKey = keyOf(key.type.parameters.first(), key.name)
                    return LinkedLazyBinding<Any?>(this, instanceKey) as LinkedBinding<T>
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
            if (binding != null) return addJustInTimeBinding(key, binding)
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
        val justInTimeLookup = InjektPlugins.justInTimeLookupFactory.findBindingForKey<T>(key)
        if (justInTimeLookup != null) {
            var binding = justInTimeLookup.binding
            return if (justInTimeLookup.scope != null) {
                val component = findComponentForScope(justInTimeLookup.scope)
                    ?: error("Couldn't find component for ${justInTimeLookup.scope}")
                binding = binding.asScoped()
                component.addJustInTimeBinding(key, binding)
            } else {
                binding.unscoped = true
                addJustInTimeBinding(key, binding)
            }
        }

        return null
    }

    private fun <T> addJustInTimeBinding(
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
        if (scope in scopes) return this
        for (dependency in dependencies) {
            dependency.findComponentForScope(scope)
                ?.let { return@findComponentForScope it }
        }

        return null
    }

}