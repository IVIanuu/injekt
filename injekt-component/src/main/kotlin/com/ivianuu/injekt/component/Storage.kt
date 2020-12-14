@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given

interface Storage<N : Component.Name> {
    operator fun <T : Any> get(key: Int): T?
    operator fun <T : Any> set(key: Int, value: T)
    fun dispose()
    interface Disposable {
        fun dispose()
    }
}

fun <N : Component.Name> Storage(): Storage<N> = StorageImpl()

inline fun <T : Any> Storage<*>.memo(key: Int, block: () -> T): T {
    get<T>(key)?.let { return it }
    synchronized(this) {
        get<T>(key)?.let { return it }
        val value = block()
        set(key, value)
        return value
    }
}

inline fun <T : Any> Storage<*>.memo(key: Any, block: () -> T): T = memo(key.hashCode(), block)

private class StorageImpl<N : Component.Name>(
    private val backing: MutableMap<Int, Any?> = mutableMapOf(),
) : Storage<N> {
    override fun <T : Any> get(key: Int): T? = backing[key] as? T
    override fun <T : Any> set(key: Int, value: T) {
        backing[key] = value
    }

    override fun dispose() {
        backing.toList().forEach { (_, value) ->
            (value as? Storage.Disposable)?.dispose()
        }
        backing.clear()
    }
}

@Given inline val <N : Component.Name> @Given Component<N>.givenStorage: Storage<N>
    get() = storage
