/*
 * Copyright 2019 Manuel Wrage
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

internal class UnlinkedComponentBinding : UnlinkedBinding<Component>() {
    override fun link(linker: Linker): LinkedBinding<Component> =
        LinkedComponentBinding(linker.component)
}

internal class LinkedComponentBinding(private val component: Component) :
    LinkedBinding<Component>() {
    override fun invoke(parameters: ParametersDefinition?): Component =
        component
}

internal class LinkedInstanceBinding<T>(private val value: T) : LinkedBinding<T>() {
    override fun invoke(parameters: ParametersDefinition?) = value
}

internal class UnlinkedProxyBinding<T>(private val originalKey: Key) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        linker.get(originalKey)
}

internal class LinkedProviderBinding<T>(
    private val component: Component,
    private val key: Key
) : LinkedBinding<Provider<T>>() {
    override fun invoke(parameters: ParametersDefinition?): Provider<T> =
        KeyedProvider(component, key)
}

internal class LinkedLazyBinding<T>(
    private val component: Component,
    private val key: Key
) : LinkedBinding<Lazy<T>>() {
    override fun invoke(parameters: ParametersDefinition?): Lazy<T> =
        KeyedLazy(component, key)
}

internal class UnlinkedMapOfProviderBinding<K, V>(
    private val entryKeys: Map<K, Key>
) : UnlinkedBinding<Map<K, Provider<V>>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, Provider<V>>> {
        return LinkedInstanceBinding(
            entryKeys.mapValues { linker.get(it.value) }
        )
    }
}

internal class UnlinkedMapOfValueBinding<K, V>(
    private val mapOfProviderKey: Key
) : UnlinkedBinding<Map<K, Lazy<V>>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, Lazy<V>>> =
        LinkedMapOfValueBinding(linker.get(mapOfProviderKey))
}

internal class LinkedMapOfValueBinding<K, V>(
    private val mapOfProviderBinding: LinkedBinding<Map<K, Provider<V>>>
) : LinkedBinding<Map<K, V>>() {
    override fun invoke(parameters: ParametersDefinition?) = mapOfProviderBinding()
        .mapValues { it.value() }
}

internal class UnlinkedMapOfLazyBinding<K, V>(
    private val mapOfProviderKey: Key
) : UnlinkedBinding<Map<K, Lazy<V>>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, Lazy<V>>> =
        LinkedMapOfLazyBinding(linker.get(mapOfProviderKey))
}

internal class LinkedMapOfLazyBinding<K, V>(
    private val mapOfProviderBinding: LinkedBinding<Map<K, Provider<V>>>
) : LinkedBinding<Map<K, Lazy<V>>>() {
    override fun invoke(parameters: ParametersDefinition?) = mapOfProviderBinding()
        .mapValues { ProviderLazy(it.value) }
}

internal class UnlinkedSetOfProviderBinding<E>(
    private val elementKeys: Set<Key>
) : UnlinkedBinding<Set<Provider<E>>>() {
    override fun link(linker: Linker): LinkedBinding<Set<Provider<E>>> {
        return LinkedInstanceBinding(
            elementKeys
                .map { linker.get<E>(it) }
                .toSet()
        )
    }
}

internal class UnlinkedSetOfValueBinding<E>(
    private val setOfProviderKey: Key
) : UnlinkedBinding<Set<Lazy<E>>>() {
    override fun link(linker: Linker): LinkedBinding<Set<Lazy<E>>> =
        LinkedSetOfValueBinding(linker.get(setOfProviderKey))
}

internal class LinkedSetOfValueBinding<E>(
    private val setOfProviderBinding: LinkedBinding<Set<Provider<E>>>
) : LinkedBinding<Set<E>>() {
    override fun invoke(parameters: ParametersDefinition?): Set<E> {
        return setOfProviderBinding()
            .map { it() }
            .toSet()
    }
}

internal class UnlinkedSetOfLazyBinding<E>(
    private val setOfProviderKey: Key
) : UnlinkedBinding<Set<Lazy<E>>>() {
    override fun link(linker: Linker): LinkedBinding<Set<Lazy<E>>> =
        LinkedSetOfLazyBinding(linker.get(setOfProviderKey))
}

internal class LinkedSetOfLazyBinding<E>(
    private val setOfProviderBinding: LinkedBinding<Set<Provider<E>>>
) : LinkedBinding<Set<Lazy<E>>>() {
    override fun invoke(parameters: ParametersDefinition?): Set<Lazy<E>> {
        return setOfProviderBinding()
            .map { ProviderLazy(it) }
            .toSet()
    }
}