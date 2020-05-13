package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class MapProvider<K, V>(private val providers: Map<K, () -> V>) : @Provider () -> Map<K, V> {
    override fun invoke(): Map<K, V> =
        providers.mapValues { it.value() }

    companion object {
        private val EMPTY = MapProvider<Any?, Any?>(emptyMap())

        fun <K, V> create(
            pair: Pair<K, () -> V>
        ): MapProvider<K, V> = MapProvider(mapOf(pair))

        fun <K, V> create(
            vararg pairs: Pair<K, () -> V>
        ): MapProvider<K, V> = MapProvider(mapOf(*pairs))

        fun <K, V> empty(): MapProvider<K, V> = EMPTY as MapProvider<K, V>

    }
}
