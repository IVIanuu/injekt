package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Key
import kotlin.reflect.KClass

class JitBindingLookup<T>(
    val scope: KClass<*>,
    val binding: Binding<T>
)

object JitBindingRegistry {

    private val bindings = mutableMapOf<Key<*>, () -> JitBindingLookup<*>>()

    fun <T> register(key: Key<T>, factory: () -> JitBindingLookup<T>) {
        bindings[key] = factory
    }

    fun <T> find(key: Key<T>): JitBindingLookup<T>? =
        bindings[key]?.invoke() as? JitBindingLookup<T>

}
