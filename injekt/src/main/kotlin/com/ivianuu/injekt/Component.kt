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

    internal val instances = mutableMapOf<Key, Instance<*>>()

    init {
        bindings.forEach { (key, binding) ->
            instances[key] = binding.createInstance(this)
        }

        instances.forEach { it.value.onAttach(this) }
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
        getInstance<T>(key).resolve(this, parameters) // todo do pass the correct component

    /**
     * Retrieve a instance of type [T]
     *
     * @param key the of the instance
     * @return the instance
     */
    fun <T> getInstance(key: Key): Instance<T> {
        val instance = findInstance<T>(key)
        if (instance != null) return instance
        if (key.type.isNullable) {
            val nullableKey = key.copy(type = key.type.copy(isNullable = true))
            return findInstance(nullableKey) ?: NullInstance as Instance<T>
        }

        error("Couldn't find a binding for $key")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findInstance(key: Key): Instance<T>? {
        var instance: Instance<T>?

        // providers, lazy
        instance = findSpecialInstance(key)
        if (instance != null) return instance

        instance = findInstanceInThisComponent(key)
        if (instance != null) return instance

        instance = findJustInTimeInstance(key)
        if (instance != null) return instance

        for (dependency in dependencies) {
            instance = dependency.findInstance(key)
            if (instance != null) return instance
        }

        return null
    }

    private fun <T> findSpecialInstance(key: Key): Instance<T>? {
        if (key.type.arguments.size == 1) {
            when (key.type.classifier) {
                Provider::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return ProviderInstance<T>(instanceKey) as Instance<T>
                }
                Lazy::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return LazyInstance<T>(instanceKey) as Instance<T>
                }
            }
        }

        return null
    }

    private fun <T> findInstanceInThisComponent(key: Key): Instance<T>? =
        synchronized(instances) { instances[key] } as? Instance<T>

    private fun <T> findJustInTimeInstance(key: Key): Instance<T>? {
        val binding = InjektPlugins.justInTimeLookupFactory.findBindingForKey<T>(key)
        if (binding == null ||
            (binding.scoping is Scoping.Scoped && binding.scoping.name !in scopes)
        ) return null
        val instance = binding.createInstance(this)
        synchronized(instances) { instances[key] = instance }
        return instance
    }

    private object NullInstance : Instance<Any?> {
        override fun resolve(component: Component, parameters: Parameters): Any? = null
    }

}
