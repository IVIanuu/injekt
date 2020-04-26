package com.ivianuu.injekt

import kotlin.reflect.KClass

class Linker(private val component: Component) {
    fun <T> get(key: Key<T>): LinkedBinding<T> = component.getBinding(key)
}

inline fun <reified T> Linker.get(qualifier: KClass<*>? = null): LinkedBinding<T> =
    get(keyOf(qualifier))
