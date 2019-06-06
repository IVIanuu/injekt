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

internal class MapBinding<K, V>(private val bindings: Map<K, Binding<V>>) :
    Binding<Map<K, V>>() {
    override fun get(parameters: ParametersDefinition?): Map<K, V> = bindings
        .mapValues { it.value.get() }
}

internal class LazyMapBinding<K, V>(private val bindings: Map<K, Binding<V>>) :
    Binding<Map<K, Lazy<V>>>() {
    override fun get(parameters: ParametersDefinition?): Map<K, Lazy<V>> = bindings
        .mapValues { (_, binding) -> lazy { binding.get() } }
}

internal class ProviderMapBinding<K, V>(private val bindings: Map<K, Binding<V>>) :
    Binding<Map<K, Provider<V>>>() {
    override fun get(parameters: ParametersDefinition?): Map<K, Provider<V>> = bindings
        .mapValues { (_, binding) -> provider { binding.get(it) } }
}

internal class SetBinding<E>(private val bindings: Set<Binding<E>>) : Binding<Set<E>>() {
    override fun get(parameters: ParametersDefinition?): Set<E> = bindings
        .map { it.get() }
        .toSet()
}

internal class LazySetBinding<E>(private val bindings: Set<Binding<E>>) :
    Binding<Set<Lazy<E>>>() {
    override fun get(parameters: ParametersDefinition?): Set<Lazy<E>> = bindings
        .map { lazy(LazyThreadSafetyMode.NONE) { it.get() } }
        .toSet()
}

internal class ProviderSetBinding<E>(private val bindings: Set<Binding<E>>) :
    Binding<Set<Provider<E>>>() {
    override fun get(parameters: ParametersDefinition?): Set<Provider<E>> = bindings
        .map { binding -> provider { binding.get(it) } }
        .toSet()
}