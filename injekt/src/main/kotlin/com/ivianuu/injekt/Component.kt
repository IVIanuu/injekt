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
    internal val scopes: List<Any>,
    internal val dependencies: List<Component>,
    internal val bindings: MutableMap<Key, Binding<*>>,
    internal val multiBindingMaps: Map<Key, MultiBindingMap<Any?, Any?>>,
    internal val multiBindingSets: Map<Key, MultiBindingSet<Any?>>
) {

    init {
        bindings
            .mapNotNull { it.value.provider as? ComponentInitObserver }
            .forEach { it.onInit(this) }
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
        getBinding<T>(key).provider(this, parameters)

    /**
     * Retrieve a binding of type [T]
     *
     * @param key the of the instance
     * @return the instance
     */
    fun <T> getBinding(key: Key): Binding<T> {
        val binding = findBinding<T>(key)
        if (binding != null) return binding

        if (key.type.isNullable) {
            return Binding<Any?>(
                key = key,
                provider = { null }
            ) as Binding<T>
        }

        error("Couldn't find a binding for $key")
    }

    fun getComponentForScope(scope: Any): Component =
        findComponentForScope(scope) ?: error("Couldn't find component for scope $scope")

    private fun findComponentForScope(scope: Any): Component? {
        if (scope in scopes) return this

        for (i in dependencies.size - 1 downTo 0) {
            dependencies[i].findComponentForScope(scope)?.let { return it }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findBinding(key: Key): Binding<T>? {
        var binding: Binding<T>?

        // providers, lazy
        binding = findSpecialBinding(key)
        if (binding != null) return binding

        binding = findBindingInThisComponent(key)
        if (binding != null) return binding

        for (i in dependencies.size - 1 downTo 0) {
            binding = dependencies[i].findBinding(key)
            if (binding != null) return binding
        }

        binding = findJustInTimeBinding(key)
        if (binding != null) return binding

        return null
    }

    private fun <T> findSpecialBinding(key: Key): Binding<T>? {
        if (key.type.arguments.size == 1) {
            when (key.type.classifier) {
                Provider::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return Binding<Provider<Any?>>(
                        key = key,
                        provider = { KeyedProvider(this, instanceKey) }
                    ) as Binding<T>
                }
                Lazy::class -> {
                    val instanceKey = keyOf(key.type.arguments.single(), key.name)
                    return Binding<Lazy<Any?>>(
                        key = key,
                        provider = { KeyedLazy(this, instanceKey) }
                    ) as Binding<T>
                }
            }
        }

        return null
    }

    private fun <T> findBindingInThisComponent(key: Key): Binding<T>? =
        bindings[key] as? Binding<T>

    private fun <T> findJustInTimeBinding(key: Key): Binding<T>? {
        if (key.name != null) return null

        val binding = InjektPlugins.justInTimeLookupFactory.findBinding(key.type) as? Binding<T>
            ?: return null
        if (binding.scoping == Scoping.Unscoped ||
            ((binding.scoping as Scoping.Scoped).name == null ||
                    binding.scoping.name in scopes)
        ) {
            bindings[key] = binding
        }
        return binding

    }

}

operator fun Component.plus(other: Component): Component {
    return Component { dependencies(this@plus, other) }
}
