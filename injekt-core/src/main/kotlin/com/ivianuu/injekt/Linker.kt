package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

class Linker(private val component: Component) {
    fun <T> get(key: Key<T>): LinkedBinding<T> = component.getBinding(key)
    inline fun <reified T> get(qualifier: KClass<*>? = null): LinkedBinding<T> =
        injektIntrinsic()
}

