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

class MapBinding<K, V>(private val bindings: Map<K, Binding<V>>) : Binding<Map<K, V>> {
    override fun link(linker: Linker) = bindings.forEach { it.value.link(linker) }

    override fun get(parameters: ParametersDefinition?): Map<K, V> = bindings
        .mapValues { it.value.get() }
}

class LazyMapBinding<K, V>(private val bindings: Map<K, Binding<V>>) : Binding<Map<K, Lazy<V>>> {
    override fun link(linker: Linker) = bindings.forEach { it.value.link(linker) }

    override fun get(parameters: ParametersDefinition?): Map<K, Lazy<V>> = bindings
        .mapValues { (_, binding) -> lazy { binding.get() } }
}

class ProviderMapBinding<K, V>(private val bindings: Map<K, Binding<V>>) :
    Binding<Map<K, Provider<V>>> {
    override fun link(linker: Linker) = bindings.forEach { it.value.link(linker) }

    override fun get(parameters: ParametersDefinition?): Map<K, Provider<V>> = bindings
        .mapValues { (_, binding) -> provider { binding.get(it) } }
}

class SetBinding<T>(private val bindings: Set<Binding<T>>) : Binding<Set<T>> {
    override fun link(linker: Linker) = bindings.forEach { it.link(linker) }

    override fun get(parameters: ParametersDefinition?): Set<T> = bindings
        .map { it.get() }
        .toSet()
}

class LazySetBinding<T>(private val bindings: Set<Binding<T>>) : Binding<Set<Lazy<T>>> {
    override fun link(linker: Linker) = bindings.forEach { it.link(linker) }

    override fun get(parameters: ParametersDefinition?): Set<Lazy<T>> = bindings
        .map { lazy(LazyThreadSafetyMode.NONE) { it.get() } }
        .toSet()
}

class ProviderSetBinding<T>(private val bindings: Set<Binding<T>>) : Binding<Set<Provider<T>>> {
    override fun link(linker: Linker) = bindings.forEach { it.link(linker) }

    override fun get(parameters: ParametersDefinition?): Set<Provider<T>> = bindings
        .map { binding -> provider { binding.get(it) } }
        .toSet()
}