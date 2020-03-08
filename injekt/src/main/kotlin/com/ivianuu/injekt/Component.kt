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
 *     single { Api(get()) }
 *     single { Database(get(), get()) }
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
    internal val scopes: List<Scope>,
    internal val dependencies: List<Component>,
    internal val bindings: MutableMap<Key<*>, Binding<*>>,
    internal val multiBindingMaps: Map<Key<*>, MultiBindingMap<Any?, Any?>>,
    internal val multiBindingSets: Map<Key<*>, MultiBindingSet<Any?>>
) {

    init {
        bindings
            .mapNotNull { it.value.provider as? ComponentInitObserver }
            .forEach { it.onInit(this) }
    }

    inline fun <reified T> get(
        qualifier: Qualifier = Qualifier.None,
        parameters: Parameters = emptyParameters()
    ): T = get(key = keyOf(qualifier = qualifier), parameters = parameters)

    /**
     * Retrieve a instance of type [T] for [key]
     */
    fun <T> get(key: Key<T>, parameters: Parameters = emptyParameters()): T =
        getBinding(key).provider(this, parameters)

    /**
     * Retrieve the [Binding] for [key] or throws
     */
    fun <T> getBinding(key: Key<T>): Binding<T> =
        findBinding(key) ?: error("Couldn't find a binding for $key")

    /**
     * Returns the [Component] for [scope] or throws
     */
    fun getComponentForScope(scope: Scope): Component =
        findComponentForScope(scope) ?: error("Couldn't find component for scope $scope")

    private fun findComponentForScope(scope: Scope): Component? {
        if (scope in scopes) return this

        for (i in dependencies.size - 1 downTo 0) {
            dependencies[i].findComponentForScope(scope)?.let { return it }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findBinding(key: Key<T>): Binding<T>? {
        var binding: Binding<T>?

        // providers, lazy
        binding = findSpecialBinding(key)
        if (binding != null) return binding

        binding = bindings[key] as? Binding<T>
        if (binding != null && !key.isNullable && binding.key.isNullable) {
            binding = null
        }
        if (binding != null) return binding

        for (i in dependencies.size - 1 downTo 0) {
            binding = dependencies[i].findBinding(key)
            if (binding != null) return binding
        }

        binding = findJustInTimeBinding(key)
        if (binding != null) return binding

        if (key.isNullable) {
            return Binding(
                key = key as Key<Any?>,
                provider = { null }
            ) as Binding<T>
        }

        return null
    }

    private fun <T> findSpecialBinding(key: Key<T>): Binding<T>? {
        if (key.arguments.size == 1) {
            when (key.classifier) {
                Provider::class -> {
                    val instanceKey = key.arguments.single()
                        .copy(qualifier = key.qualifier)
                    return Binding(
                        key = key,
                        provider = { KeyedProvider(this, instanceKey) as T }
                    )
                }
                Lazy::class -> {
                    val instanceKey = key.arguments.single()
                        .copy(qualifier = key.qualifier)
                    return Binding(
                        key = key,
                        provider = { KeyedLazy(this, instanceKey) as T }
                    )
                }
            }
        }

        return null
    }

    private fun <T> findJustInTimeBinding(key: Key<T>): Binding<T>? {
        if (key.qualifier != Qualifier.None) return null

        val binding = InjektPlugins.justInTimeBindingFactory.findBinding(key)
            ?: return null
        bindings[key] = binding
        (binding.provider as? ComponentInitObserver)?.onInit(this)
        return binding
    }
}

operator fun Component.plus(other: Component): Component {
    return Component { dependencies(this@plus, other) }
}
