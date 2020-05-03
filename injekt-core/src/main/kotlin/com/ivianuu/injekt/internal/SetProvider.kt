package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class SetProvider<E>(private val providers: Set<Provider<E>>) : Provider<Set<E>> {
    override fun invoke(): Set<E> =
        providers.mapTo(LinkedHashSet(providers.size)) { it() }

    companion object {
        private val EMPTY = SetProvider<Any?>(emptySet())

        fun <E> create(element: Provider<E>): SetProvider<E> = SetProvider(setOf(element))

        fun <E> create(vararg elements: Provider<E>): SetProvider<E> =
            SetProvider(setOf(*elements))

        fun <E> empty(): SetProvider<E> = EMPTY as SetProvider<E>

    }
}