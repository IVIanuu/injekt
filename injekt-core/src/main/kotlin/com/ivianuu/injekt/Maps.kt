package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

@InjektDslMarker
interface MapDsl<K, V> {
    fun <T : V> put(entryKey: K): Unit = injektIntrinsic()
}

@Declaration
fun <K, V> map(
    mapQualifier: KClass<*>? = null,
    block: MapDsl<K, V>.() -> Unit = {}
): Unit = injektIntrinsic()
