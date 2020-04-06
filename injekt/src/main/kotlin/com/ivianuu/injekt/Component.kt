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
    val jitFactories: List<JitFactory>,
    bindings: MutableMap<Key<*>, Binding<*>>
) {

    private val _bindings = bindings
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

    private var initializedBindings: MutableSet<Key<*>>? = mutableSetOf()

    init {
        for (binding in _bindings.values.toList()) {
            val initializedBindings = initializedBindings!!
            if (binding.key !in initializedBindings) {
                initializedBindings += binding.key
                binding.provider.onAttach(this)
            }
        }
        initializedBindings = null // Don't needed anymore
    }

    /**
     * Retrieve a instance of type [T] for [key]
     */
    @KeyOverload
    fun <T> get(key: Key<T>, parameters: Parameters = emptyParameters()): T {
        findExplicitBinding(key)?.let { return it.provider(this, parameters) }
        findJitBinding(key)?.let { return it.provider(this, parameters) }
        if (key.isNullable) return null as T
        error("Couldn't get instance for $key")
    }

    /**
     * Returns the [Component] for [scope] or throws
     */
    fun getComponent(scope: Scope): Component =
        findComponent(scope) ?: error("Couldn't find component for scope $scope")

    private fun findComponent(scope: Scope): Component? {
        if (scope in scopes) return this

        for (index in parents.indices) {
            parents[index].findComponent(scope)?.let { return it }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findExplicitBinding(key: Key<T>): Binding<T>? {
        var binding = synchronized(_bindings) { _bindings[key] } as? Binding<T>
        if (binding != null && !key.isNullable && binding.key.isNullable) {
            binding = null
        }
        if (binding != null) {
            initializedBindings?.let {
                // we currently initialize bindings
                // make sure that the requested binding gets also initialized
                if (binding!!.key !in it) {
                    it += binding!!.key
                    binding!!.provider.onAttach(this)
                }
            }
            return binding
        }

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
                val component = if (binding.scope != null) {
                    getComponent(binding.scope)
                } else {
                    this
                }
                synchronized(component._bindings) { component._bindings[key] = binding }
                initializedBindings?.let { it += key }
                binding.provider.onAttach(component)
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

@Module(invokeOnInit = true)
private fun ComponentBuilder.componentModule() {
    bind(
        tag = Bound,
        duplicateStrategy = DuplicateStrategy.Override
    ) { this }

    onScopeAdded { scope ->
        bind(
            qualifier = scope,
            scope = scope,
            tag = Bound,
            duplicateStrategy = DuplicateStrategy.Override
        ) { this }
    }
}
