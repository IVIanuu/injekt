package com.ivianuu.injekt

import kotlin.reflect.KClass

inline class Linker(private val component: Component) {
    fun <T> get(key: Key<T>): Provider<T> = component.getProvider(key)
}

inline fun <reified T> Linker.get(qualifier: KClass<*>? = null): Provider<T> = get(keyOf(qualifier))

interface Linkable {
    fun link(linker: Linker)
}
