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
    internal val bindings: Map<Key, Binding<*>>,
    internal val multiBindingMaps: Map<Key, MultiBindingMap<Any?, Any?>>,
    internal val multiBindingSets: Map<Key, MultiBindingSet<Any?>>,
    internal val dependencies: Set<Component>
) {

    internal val providers = mutableMapOf<Key, BindingProvider<*>>()

    init {
        bindings.forEach { (key, binding) ->
            providers[key] = binding.kind.wrap(binding as Binding<Any?>, binding.provider)
        }

        providers.forEach { it.value.onAttach(this) }
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
        getProvider<T>(key).resolve(this, parameters) // todo do pass the correct component

    /**
     * Retrieve a [BindingProvider] of type [T]
     *
     * @param key the of the instance
     * @return the instance
     */
    fun <T> getProvider(key: Key): BindingProvider<T> {
        val instance = findProvider<T>(key)
        if (instance != null) return instance
        if (key.type.isNullable) {
            val nullableKey = key.copy(type = key.type.copy(isNullable = true))
            return findProvider(nullableKey) ?: NullBindingProvider as BindingProvider<T>
        }

        error("Couldn't find a binding for $key")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findProvider(key: Key): BindingProvider<T>? {
        var provider: BindingProvider<T>?

        // providers, lazy
        provider = findSpecialProvider(key)
        if (provider != null) return provider

        provider = findProviderInThisComponent(key)
        if (provider != null) return provider

        provider = findJustInTimeInstance(key)
        if (provider != null) return provider

        for (dependency in dependencies) {
            provider = dependency.findProvider(key)
            if (provider != null) return provider
        }

        return null
    }

    private fun <T> findSpecialProvider(key: Key): BindingProvider<T>? {
        if (key.type.arguments.size == 1) {
            when (key.type.classifier) {
                Provider::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return BindingProviderProvider<T>(instanceKey) as BindingProvider<T>
                }
                Lazy::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return LazyBindingProvider<T>(instanceKey) as BindingProvider<T>
                }
            }
        }

        return null
    }

    private fun <T> findProviderInThisComponent(key: Key): BindingProvider<T>? =
        synchronized(providers) { providers[key] } as? BindingProvider<T>

    private fun <T> findJustInTimeInstance(key: Key): BindingProvider<T>? {
        val binding = InjektPlugins.justInTimeLookupFactory.findBindingForKey<T>(key)
        if (binding == null ||
            (binding.scoping is Scoping.Scoped && binding.scoping.name !in scopes)
        ) return null
        val provider = binding.kind.wrap(binding, binding.provider)
        synchronized(providers) { providers[key] = provider }
        return provider
    }

    private object NullBindingProvider : BindingProvider<Any?> {
        override fun resolve(component: Component, parameters: Parameters): Any? = null
    }

}
