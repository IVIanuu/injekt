package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Key
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class JitBindingMetadata(
    val type: KClass<*>,
    val binding: KClass<*>
)

object JitBindingRegistry {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()

    fun <T> register(key: Key<T>, binding: Binding<T>) {
        bindings[key] = binding
    }

    fun <T> find(key: Key<T>): Binding<T>? = bindings[key] as? Binding<T>

}
