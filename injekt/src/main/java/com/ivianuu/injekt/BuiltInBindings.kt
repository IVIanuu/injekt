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

internal class BindingProvider<T>(private val binding: LinkedBinding<T>) : Provider<T> {
    override fun invoke(parameters: ParametersDefinition?): T = binding.get(parameters)
}

internal class LinkedProviderBindingWrapper<T>(
        private val binding: LinkedBinding<T>
) : LinkedBinding<Provider<T>>() {
    override fun get(parameters: ParametersDefinition?): Provider<T> =
            BindingProvider(binding)
}

internal class LinkedProviderBinding<T>(
    private val component: Component,
    private val key: Key
) : LinkedBinding<Provider<T>>() {
    override fun get(parameters: ParametersDefinition?): Provider<T> =
        KeyedProvider(component, key)
}

private class KeyedProvider<T>(
    private val component: Component,
    private val key: Key
) : Provider<T> {

    private var _binding: LinkedBinding<T>? = null

    override fun invoke(parameters: ParametersDefinition?): T {
        var binding = _binding
        if (binding == null) {
            binding = component.getBinding(key)
            _binding = binding
        }
        return binding.get(parameters)
    }
}

internal class LinkedLazyBinding<T>(
    private val component: Component,
    private val key: Key
) : LinkedBinding<Lazy<T>>() {
    override fun get(parameters: ParametersDefinition?): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
        component.getBinding<T>(key).get()
    }
}

internal class UnlinkedMapBinding<K, V>(private val keysByKey: Map<K, Key>) :
    UnlinkedBinding<Map<K, V>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, V>> =
        LinkedMapBinding(keysByKey.mapValues { linker.get<V>(it.value) })
}

internal class LinkedMapBinding<K, V>(private val bindingsByKey: Map<K, LinkedBinding<out V>>) :
    LinkedBinding<Map<K, V>>() {
    override fun get(parameters: ParametersDefinition?): Map<K, V> =
        bindingsByKey.mapValues { it.value.get() }

    fun asLinkedProviderMapBinding(): LinkedBinding<Map<K, Provider<V>>> {
        val providersByKey = bindingsByKey
                .mapValues { LinkedProviderBindingWrapper(it.value) }
        return LinkedProviderMapBinding(providersByKey as Map<K, LinkedBinding<Provider<V>>>)
    }
}

internal class LinkedProviderMapBinding<K, V>(
        private val providersByKey: Map<K, LinkedBinding<Provider<V>>>
) : LinkedBinding<Map<K, Provider<V>>>() {
    override fun get(parameters: ParametersDefinition?): Map<K, Provider<V>> = providersByKey
            .mapValues { it.value.get() }
}

internal class UnlinkedSetBinding<E>(private val keys: Set<Key>) : UnlinkedBinding<Set<E>>() {
    override fun link(linker: Linker): LinkedBinding<Set<E>> =
        LinkedSetBinding(keys.map { linker.get<E>(it) }.toSet())
}

internal class LinkedSetBinding<E>(private val bindings: Set<LinkedBinding<out E>>) :
    LinkedBinding<Set<E>>() {
    override fun get(parameters: ParametersDefinition?): Set<E> =
        bindings.map { it.get() }.toSet()

    fun asLinkedProviderSetBinding(): LinkedBinding<Set<Provider<E>>> {
        val providers = bindings
                .map { LinkedProviderBindingWrapper(it) }
                .toSet()
        return LinkedProviderSetBinding(providers as Set<LinkedBinding<Provider<E>>>)
    }
}

internal class LinkedProviderSetBinding<E>(
        private val providers: Set<LinkedBinding<Provider<E>>>
) : LinkedBinding<Set<Provider<E>>>() {
    override fun get(parameters: ParametersDefinition?): Set<Provider<E>> =
            providers.map { it.get() }.toSet()
}

internal class InstanceBinding<T>(private val instance: T) : LinkedBinding<T>() {
    override fun get(parameters: ParametersDefinition?): T = instance
}