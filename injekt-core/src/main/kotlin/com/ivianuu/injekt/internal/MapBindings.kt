package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.UnlinkedBinding

internal class MapOfProviderBinding<K, V>(
    private val mapOfKey: Map<K, Key<V>>
) : UnlinkedBinding<Map<K, Provider<V>>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, Provider<V>>> =
        InstanceBinding(mapOfKey.mapValues { linker.get(it.value) })
}

internal class MapOfValueBinding<K, V>(
    private val mapOfProviderKey: Key<Map<K, Provider<V>>>
) : UnlinkedBinding<Map<K, V>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, V>> {
        return Linked(linker.get(mapOfProviderKey))
    }

    private class Linked<K, V>(private val mapOfProviderProvider: Provider<Map<K, Provider<V>>>) :
        LinkedBinding<Map<K, V>>() {
        override fun invoke(parameters: Parameters): Map<K, V> =
            mapOfProviderProvider().mapValues { it.value() }
    }
}

internal class MapOfLazyBinding<K, V>(
    private val mapOfProviderKey: Key<Map<K, Provider<V>>>
) : UnlinkedBinding<Map<K, Lazy<V>>>() {
    override fun link(linker: Linker): LinkedBinding<Map<K, Lazy<V>>> {
        return Linked(linker.get(mapOfProviderKey))
    }

    private class Linked<K, V>(private val mapOfProviderProvider: Provider<Map<K, Provider<V>>>) :
        LinkedBinding<Map<K, Lazy<V>>>() {
        override fun invoke(parameters: Parameters): Map<K, Lazy<V>> =
            mapOfProviderProvider().mapValues { ProviderLazy(it.value) }
    }
}
