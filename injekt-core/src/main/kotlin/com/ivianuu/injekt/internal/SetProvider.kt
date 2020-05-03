package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class SetProvider<T>(private val providers: Set<Provider<T>>) : Provider<Set<T>> {
    override fun invoke(): Set<T> =
        providers.mapTo(LinkedHashSet(providers.size)) { it() }

    companion object {
        private val EMPTY = SetProvider<Any?>(emptySet())

        fun <T> create(provider: Provider<T>): SetProvider<T> = SetProvider(setOf(provider))

        fun <T> create(vararg providers: Provider<T>): SetProvider<T> =
            SetProvider(setOf(*providers))

        fun <T> empty(): SetProvider<T> = EMPTY as SetProvider<T>

    }
}