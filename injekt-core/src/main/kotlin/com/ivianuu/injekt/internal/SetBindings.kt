package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.UnlinkedBinding

internal class SetOfProviderBinding<E>(
    private val setOfKey: Set<Key<E>>
) : UnlinkedBinding<Set<Provider<E>>>() {
    override fun link(linker: Linker): LinkedBinding<Set<Provider<E>>> =
        InstanceBinding(setOfKey.mapTo(LinkedHashSet(setOfKey.size)) { linker.get(it) })
}

internal class SetOfValueBinding<E>(
    private val setOfProviderKey: Key<Set<Provider<E>>>
) : UnlinkedBinding<Set<E>>() {
    override fun link(linker: Linker): LinkedBinding<Set<E>> {
        return Linked(linker.get(setOfProviderKey))
    }

    private class Linked<E>(private val setOfProviderProvider: Provider<Set<Provider<E>>>) :
        LinkedBinding<Set<E>>() {
        override fun invoke(parameters: Parameters): Set<E> =
            setOfProviderProvider().let { it.mapTo(LinkedHashSet(it.size)) { it() } }
    }
}

internal class SetOfLazyBinding<E>(
    private val setOfProviderKey: Key<Set<Provider<E>>>
) : UnlinkedBinding<Set<Lazy<E>>>() {
    override fun link(linker: Linker): LinkedBinding<Set<Lazy<E>>> {
        return Linked(linker.get(setOfProviderKey))
    }

    private class Linked<E>(private val setOfProviderProvider: Provider<Set<Provider<E>>>) :
        LinkedBinding<Set<Lazy<E>>>() {
        override fun invoke(parameters: Parameters): Set<Lazy<E>> =
            setOfProviderProvider().let { it.mapTo(LinkedHashSet(it.size)) { ProviderLazy(it) } }
    }
}
