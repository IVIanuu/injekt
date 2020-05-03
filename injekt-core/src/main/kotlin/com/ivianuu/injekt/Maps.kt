package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@InjektDslMarker
interface MapDsl<K, V> {
    fun <T : V> put(entryKey: K)
    // todo fun <T : V> put(entryKey: K, definition: ProviderDefinition<T>)
}

@Declaration
fun <K, V> map(block: MapDsl<K, V>.() -> Unit = {}): Unit = injektIntrinsic()
