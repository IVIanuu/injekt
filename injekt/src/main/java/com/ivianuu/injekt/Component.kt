/*
 * Copyright 2018 Manuel Wrage
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
 * The actual dependency container which provides bindings
 * Dependencies can be requested by calling either [get] or [inject]
 */
class Component internal constructor(
    val scope: Scope?,
    internal val bindings: MutableMap<Key, Binding<*>>,
    internal val dependencies: Iterable<Component>
) {

    private val linker = RealLinker(this) // todo move to constructor

    init {
        bindings.forEach { (key, binding) -> binding.link(linker) }
        bindings.values
            .filterIsInstance<AttachAware>()
            .forEach { it.attached() }
    }

    internal fun <T> getBinding(type: Type<T>, name: Qualifier? = null): Binding<T> =
        findBinding(keyOf(type, name), true) ?: error("couldn't find binding")

    /**
     * Returns the instance matching the [type] and [name]
     */
    fun <T> get(
        type: Type<T>,
        name: Qualifier? = null,
        parameters: ParametersDefinition? = null
    ): T {
        // check if we need a lazy or a provider
        if (type.parameters.size == 1) {
            when (type.raw) {
                Provider::class -> {
                    val key = keyOf(type.parameters.first(), name)
                    findBinding<T>(key, true)
                        ?.let { instance ->
                            return@get provider { instance.get(it) } as T
                        }
                }
                Lazy::class -> {
                    val key = keyOf(type.parameters.first(), name)
                    findBinding<T>(key, true)
                        ?.let {
                            return@get lazy(LazyThreadSafetyMode.NONE) { it.get(parameters) } as T
                        }
                }
            }
        }

        // just try to resolve the dependency
        val key = keyOf(type, name)
        findBinding<T>(key, true)
            ?.let { return@get it.get(parameters) }

        // todo clean up
        throw IllegalStateException("Couldn't find a binding for ${keyOf(type, name)}")
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T> findBinding(key: Key, lookupJit: Boolean): Binding<T>? {
        var binding = bindings[key]
        if (binding != null) return binding as Binding<T>

        for (dependency in dependencies) {
            binding = dependency.findBinding<T>(key, false)
            if (binding != null) return binding
        }

        if (lookupJit && key.name == null) {
            val bindingFactory = JustInTimeBindings.find<T>(key)
            if (bindingFactory != null) {
                val component = findComponentForScope(bindingFactory.scope)
                    ?: error("Couldn't find component for $scope")
                binding = bindingFactory.create()
                component.addJitBinding(key, binding)
                return binding
            }
        }

        return null
    }

    private fun <T> addJitBinding(key: Key, binding: Binding<T>) {
        bindings[key] = binding
        binding.link(linker)
        (binding as? AttachAware)?.attached()
    }

    private fun findComponentForScope(scope: Any?): Component? {
        if (scope == null) return this
        if (this.scope == scope) return this
        for (dependency in dependencies) {
            dependency.findComponentForScope(scope)
                ?.let { return@findComponentForScope it }
        }

        return null
    }

}

inline fun <reified T> Component.get(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(typeOf(), name, parameters)

inline fun <reified T> Component.inject(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(typeOf(), name, parameters)

fun <T> Component.inject(
    type: Type<T>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(type, name, parameters) }