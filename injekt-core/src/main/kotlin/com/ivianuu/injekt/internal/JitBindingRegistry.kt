package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Key

object JitBindingRegistry {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()

    fun register(key: Key<*>, binding: Binding<*>) {
        bindings[key] = binding
    }

    fun <T> find(key: Key<T>): Binding<T>? = bindings[key] as? Binding<T>

}
