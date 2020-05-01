package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@InjektDslMarker
interface MapDsl<K, V> {
    fun <T : V> put(entryKey: K): Unit = injektIntrinsic()
}

@Declaration
fun <K, V> map(block: MapDsl<K, V>.() -> Unit = {}): Unit = injektIntrinsic()
