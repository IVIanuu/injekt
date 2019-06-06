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

internal class MapBinding<K, V>(private val bindings: Map<K, MapContribution<K, V>>) :
    Binding<Map<K, V>> {
    override fun get(parameters: ParametersDefinition?): Map<K, V> = bindings
        .mapValues { it.value.binding.get() }
}

internal class LazyMapBinding<K, V>(private val bindings: Map<K, MapContribution<K, V>>) :
    Binding<Map<K, Lazy<V>>> {
    override fun get(parameters: ParametersDefinition?): Map<K, Lazy<V>> = bindings
        .mapValues { (_, contribution) -> lazy { contribution.binding.get() } }
}

internal class ProviderMapBinding<K, V>(private val bindings: Map<K, MapContribution<K, V>>) :
    Binding<Map<K, Provider<V>>> {
    override fun get(parameters: ParametersDefinition?): Map<K, Provider<V>> = bindings
        .mapValues { (_, contribution) -> provider { contribution.binding.get(it) } }
}

internal class SetBinding<T>(private val bindings: Set<SetContribution<T>>) : Binding<Set<T>> {
    override fun get(parameters: ParametersDefinition?): Set<T> = bindings
        .map { it.binding.get() }
        .toSet()
}

internal class LazySetBinding<T>(private val bindings: Set<SetContribution<T>>) :
    Binding<Set<Lazy<T>>> {
    override fun get(parameters: ParametersDefinition?): Set<Lazy<T>> = bindings
        .map { lazy(LazyThreadSafetyMode.NONE) { it.binding.get() } }
        .toSet()
}

internal class ProviderSetBinding<T>(private val bindings: Set<SetContribution<T>>) :
    Binding<Set<Provider<T>>> {
    override fun get(parameters: ParametersDefinition?): Set<Provider<T>> = bindings
        .map { contribution -> provider { contribution.binding.get(it) } }
        .toSet()
}