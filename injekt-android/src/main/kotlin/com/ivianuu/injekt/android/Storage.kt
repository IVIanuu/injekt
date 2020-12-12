@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.android

import kotlinx.coroutines.DisposableHandle

interface Storage {
    operator fun <T : Any> get(key: Int): T?
    operator fun <T : Any> set(key: Int, value: T)
    fun dispose()
}

fun Storage(): Storage = StorageImpl()

inline fun <T : Any> Storage.memo(key: Int, block: () -> T): T {
    get<T>(key)?.let { return it }
    synchronized(this) {
        get<T>(key)?.let { return it }
        val value = block()
        set(key, value)
        return value
    }
}

inline fun <T : Any> Storage.memo(key: Any, block: () -> T): T = memo(key.hashCode(), block)

private class StorageImpl(private val backing: MutableMap<Int, Any?> = mutableMapOf()) : Storage {
    override fun <T : Any> get(key: Int): T? = backing[key] as? T
    override fun <T : Any> set(key: Int, value: T) {
        backing[key] = value
    }

    override fun dispose() {
        backing.toList().forEach { (_, value) ->
            (value as? DisposableHandle)?.dispose()
        }
        backing.clear()
    }
}
