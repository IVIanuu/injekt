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

import com.jakewharton.confundus.unsafeCast

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
    val jitFactories: List<JitFactory>,
    bindings: MutableMap<Key<*>, Binding<*>>
) {

    private val _bindings = bindings
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

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
        findExplicitBinding(key)?.let { return it }
        findJitBinding(key)?.let { return it }
        if (key.isNullable) return Binding(
            key = key
        ) { null as T }.unsafeCast()
        error("Couldn't get instance for $key")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findExplicitBinding(key: Key<T>): Binding<T>? {
        var binding = synchronized(_bindings) { _bindings[key] } as? Binding<T>
        if (binding != null && !key.isNullable && binding.key.isNullable) {
            binding = null
        }
        if (binding != null) return binding

        for (index in parents.lastIndex downTo 0) {
            binding = parents[index].findExplicitBinding(key)
            if (binding != null) return binding
        }

        return null
    }

    private fun <T> findJitBinding(key: Key<T>): Binding<T>? {
        for (index in jitFactories.lastIndex downTo 0) {
            val binding = jitFactories[index].create(key, this)
            if (binding != null) {
                synchronized(_bindings) { _bindings[key] = binding }
                return binding
            }
        }

        return null
    }
}

/**
 * Holds a [Component] and allows for shorter syntax and lazy construction of a component
 *
 * Example:
 *
 * ```
 * class MainActivity : Activity(), ComponentOwner {
 *
 *     override val component = Component { ... }
 *
 *     private val dep1: Dependency1 by getLazy()
 *     private val dep2: Dependency2 by getLazy()
 *
 * }
 * ```
 *
 */
interface ComponentOwner {
    /**
     * The [Component] which will be used to retrieve instances
     */
    val component: Component
}

/**
 * @see Component.get
 */
@KeyOverload
fun <T> ComponentOwner.get(
    key: Key<T>,
    parameters: Parameters = emptyParameters()
): T = component.get(key, parameters)

/**
 * Lazy version of [get]
 *
 * @param key the key of the instance
 * @param parameters optional parameters to construct the instance
 * @return the instance

 * @see Component.get
 */
@KeyOverload
inline fun <T> ComponentOwner.getLazy(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): kotlin.Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(key, parameters()) }

@ModuleMarker
private val ComponentModule = Module(scope = AnyScope, invokeOnInit = true) {
    factory(
        behavior = Bound,
        duplicateStrategy = DuplicateStrategy.Override
    ) { this }

    onScopeAdded { scope ->
        factory(
            qualifier = scope,
            behavior = Bound,
            duplicateStrategy = DuplicateStrategy.Override
        ) { this }
    }
}
