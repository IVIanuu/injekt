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
 * Instances can be requested by calling [get]
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
    val scopes: Set<Scope>,
    val parents: List<Component>,
    val jitFactories: List<(Key<Any?>, Component) -> Binding<Any?>?>,
    val bindings: Map<Key<*>, Binding<*>>
) {

    /**
     * Return a instance of type [T] for [key]
     */
    @KeyOverload
    fun <T> get(key: Key<T>, parameters: Parameters = emptyParameters()): T =
        getBindingProvider(key)(parameters)

    /**
     * Returns the [BindingProvider] for [key]
     */
    fun <T> getBindingProvider(key: Key<T>): BindingProvider<T> = getBinding(key).provider

    /**
     * Returns the binding for [key]
     */
    fun <T> getBinding(key: Key<T>): Binding<T> {
        findBinding(key)?.let { return it }
        if (key.isNullable) return Binding(
            key = key
        ) { null as T }
        error("Couldn't get instance for $key")
    }

    private fun <T> findBinding(key: Key<T>): Binding<T>? {
        var binding = bindings[key] as? Binding<T>
        if (binding != null && !key.isNullable && binding.key.isNullable) {
            binding = null
        }
        if (binding != null) return binding

        for (index in parents.lastIndex downTo 0) {
            binding = parents[index].findBinding(key)
            if (binding != null) return binding
        }

        for (index in jitFactories.lastIndex downTo 0) {
            binding = jitFactories[index](key as Key<Any?>, this) as? Binding<T>
            if (binding != null) return binding
        }

        return null
    }

}
