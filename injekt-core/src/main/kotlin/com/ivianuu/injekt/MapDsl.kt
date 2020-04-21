package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

class MapDsl<K, V> {
    inline fun <reified T : V> put(key: K): Unit = stub()
}

//@Module
inline fun <reified K, reified V> map(
    vararg qualifiers: Qualifier,
    block: MapDsl<K, V>.() -> Unit = {}
): Unit = stub()
