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

/**
 * The heart of the library which provides instances
 * Dependencies can be requested by calling [get]
 * Use [ComponentBuilder] to construct [Component] instances
 *
 * Typical usage of a [Component] looks like this:
 *
 * ´´´
 * val component = Component {
 *     scopes(Singleton)
 *     modules(networkModule)
 *     modules(databaseModule)
 * }
 *
 * val api = component.get<Api>()
 * val database = component.get<Database>()
 * ´´´
 *
 * @see get
 * @see getLazy
 * @see ComponentBuilder
 */
class Component internal constructor(
    internal val scopes: Set<Any>,
    internal val scopedBindings: MutableMap<Key, Binding<*>>,
    internal val unlinkedUnscopedBindings: Map<Key, UnlinkedBinding<*>>,
    eagerBindings: Set<Key>,
    internal val multiBindingMaps: Map<Key, MultiBindingMap<Any?, Any?>>,
    internal val multiBindingSets: Map<Key, MultiBindingSet<Any?>>,
    internal val dependencies: Set<Component>
) {

    private val linkedBindingsByUnlinked = mutableMapOf<UnlinkedBinding<*>, LinkedBinding<*>>()

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
     * Retrieve a instance of type [T]
     *
     * @param key the of the instance
     * @param parameters optional parameters to construct the instance
     * @return the instance
     */
    fun <T> get(key: Key, parameters: ParametersDefinition? = null): T =
        getBinding<T>(key)(parameters)

    /**
     * Retrieve a binding of type [T]
     *
     * @param key the of the instance
     * @return the instance
     */
    fun <T> getBinding(key: Key): LinkedBinding<T> {
        val binding = findBinding<T>(key)
        if (binding != null) return binding
        if (key.type.isNullable) {
            val nullableKey = key.copy(type = key.type.copy(isNullable = true))
            return findBinding(nullableKey) ?: NullBinding as LinkedBinding<T>

        }

        error("Couldn't find a binding for $key")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findBinding(key: Key): LinkedBinding<T>? {
        var binding: Binding<T>?

        // providers, lazy
        binding = findSpecialBinding(key)
        if (binding != null) return binding

        binding = findScopedBinding(key)
        if (binding != null) return binding

        binding = findUnscopedBinding(key)
        if (binding != null) return binding

        for (dependency in dependencies) {
            binding = dependency.findBindingForChild(key)
            if (binding != null) return binding
        }

        binding = findJustInTimeBinding(key)
        if (binding != null) return binding

        return null
    }

    private fun <T> findBindingForChild(key: Key): LinkedBinding<T>? {
        findScopedBinding<T>(key)?.let { return it }

        dependencies.forEach { dependency ->
            dependency.findBindingForChild<T>(key)?.let { return it }
        }

        return null
    }

    private fun <T> findSpecialBinding(key: Key): LinkedBinding<T>? {
        if (key.type.parameters.size == 1) {
            when (key.type.raw) {
                Provider::class -> {
                    val instanceKey = keyOf(key.type.parameters.single(), key.name)
                    return ProviderBinding<Any?>(this, instanceKey) as LinkedBinding<T>
                }
                Lazy::class -> {
                    val instanceKey = keyOf(key.type.parameters.single(), key.name)
                    return LinkedLazyBinding<Any?>(this, instanceKey) as LinkedBinding<T>
                }
            }
        }

        return null
    }

    private fun <T> findScopedBinding(key: Key): LinkedBinding<T>? =
        synchronized(scopedBindings) { scopedBindings[key] }?.linkIfNeeded(key) as? LinkedBinding<T>

    private fun <T> findUnscopedBinding(key: Key): LinkedBinding<T>? =
        unlinkedUnscopedBindings[key]?.linkIfNeeded(key) as? LinkedBinding<T>

    private fun <T> findJustInTimeBinding(key: Key): LinkedBinding<T>? {
        val justInTimeLookup = InjektPlugins.justInTimeLookupFactory.findBindingForKey<T>(key)
        if (justInTimeLookup != null) {
            val binding = justInTimeLookup.binding
                .let { if (justInTimeLookup.isSingle) it.asSingle() else it }
            return if (justInTimeLookup.scope != null) {
                val component = findComponentForScope(justInTimeLookup.scope)
                    ?: error("Couldn't find Component for ${justInTimeLookup.scope}")
                binding.scoped = true
                component.addJustInTimeBinding(key, binding)
            } else {
                addJustInTimeBinding(key, binding)
            }
        }

        return null
    }

    private fun <T> addJustInTimeBinding(
        key: Key,
        binding: Binding<T>
    ): LinkedBinding<T> {
        val linkedBinding = binding.performLink(this)
        synchronized(scopedBindings) { scopedBindings[key] = linkedBinding }
        return linkedBinding
    }

    private fun <T> Binding<T>.linkIfNeeded(key: Key): LinkedBinding<T> {
        if (this is LinkedBinding) return this
        this as UnlinkedBinding
        val linkedBinding = linkedBindingsByUnlinked.getOrPut(this) {
            performLink(this@Component)
        } as LinkedBinding<T>
        synchronized(scopedBindings) { scopedBindings[key] = linkedBinding }
        return linkedBinding
    }

    private fun findComponentForScope(scope: Any): Component? {
        if (scope in scopes) return this
        for (dependency in dependencies) {
            dependency.findComponentForScope(scope)
                ?.let { return@findComponentForScope it }
        }

        return null
    }

    private object NullBinding : LinkedBinding<Any?>() {
        override fun invoke(parameters: ParametersDefinition?): Any? = null
    }
}
