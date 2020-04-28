package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Key

object JitBindingRegistry {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()

    init {
        println("hello world")
    }

    fun <T> register(key: Key<T>, binding: Binding<T>) {
        println("register $key")
        bindings[key] = binding
    }

    fun <T> find(key: Key<T>): Binding<T>? = bindings[key] as? Binding<T>

}
