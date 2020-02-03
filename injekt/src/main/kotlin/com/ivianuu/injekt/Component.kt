/*
 * Copyright 2020 Manuel Wrage
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
    internal val scopedBindings: Map<Key, Binding<*>>,
    internal val unlinkedUnscopedBindings: Map<Key, UnlinkedBinding<*>>,
    internal val multiBindingMaps: Map<Key, MultiBindingMap<Any?, Any?>>,
    internal val multiBindingSets: Map<Key, MultiBindingSet<Any?>>,
    internal val dependencies: Set<Component>
) {

    private val linkedBindings = mutableMapOf<Key, LinkedBinding<*>>()

    init {
        scopedBindings.forEach { (key, binding) ->
            linkedBindings[key] = binding.performLink(this)
        }
        unlinkedUnscopedBindings.forEach { (key, binding) ->
            linkedBindings[key] = binding.performLink(this)
        }

        scopedBindings
            .filter { it.value.eager }
            .forEach { get(it.key) }
    }

    inline fun <reified T> get(
        name: Any? = null,
        parameters: Parameters = emptyParameters()
    ): T = get(type = typeOf(), name = name, parameters = parameters)

    fun <T> get(
        type: Type<T>,
        name: Any? = null,
        parameters: Parameters = emptyParameters()
    ): T = get(key = keyOf(type, name), parameters = parameters)

    /**
     * Retrieve a instance of type [T]
     *
     * @param key the of the instance
     * @param parameters optional parameters to construct the instance
     * @return the instance
     */
    fun <T> get(key: Key, parameters: Parameters = emptyParameters()): T =
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

        binding = findBindingInThisComponent(key)
        if (binding != null) return binding

        binding = findJustInTimeBinding(key)
        if (binding != null) return binding

        for (dependency in dependencies) {
            binding = dependency.findBinding(key)
            if (binding != null) return binding
        }

        return null
    }

    private fun <T> findSpecialBinding(key: Key): LinkedBinding<T>? {
        if (key.type.arguments.size == 1) {
            when (key.type.classifier) {
                Provider::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return ProviderBinding<Any?>(this, instanceKey) as LinkedBinding<T>
                }
                Lazy::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return LinkedLazyBinding<Any?>(this, instanceKey) as LinkedBinding<T>
                }
            }
        }

        return null
    }

    private fun <T> findBindingInThisComponent(key: Key): LinkedBinding<T>? =
        synchronized(linkedBindings) { linkedBindings.get(key) } as? LinkedBinding<T>

    private fun <T> findJustInTimeBinding(key: Key): LinkedBinding<T>? {
        val justInTimeLookup = InjektPlugins.justInTimeLookupFactory.findBindingForKey<T>(key)
        if (justInTimeLookup == null ||
            (justInTimeLookup.scope != null && justInTimeLookup.scope !in scopes)
        ) return null

        val binding = justInTimeLookup.binding
            .performLink(this)
            .let { if (justInTimeLookup.isSingle) it.asSingle() else it } as LinkedBinding<T>

        binding.scoped = justInTimeLookup.scope != null
        synchronized(linkedBindings) { linkedBindings[key] = binding }
        return binding
    }

    private object NullBinding : LinkedBinding<Any?>() {
        override fun invoke(parameters: Parameters): Any? = null
    }
}
