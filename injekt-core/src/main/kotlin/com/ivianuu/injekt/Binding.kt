package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

sealed class Binding<T> {
    abstract fun link(linker: Linker): LinkedBinding<T>
}

abstract class UnlinkedBinding<T> : Binding<T>()

abstract class LinkedBinding<T> : Binding<T>(), Provider<T> {
    final override fun link(linker: Linker): LinkedBinding<T> = this
}

class BindingDsl {
    fun <T> get(
        key: Key<T>,
        parameters: Parameters = emptyParameters()
    ): T = injektIntrinsic()

    inline fun <reified T> get(
        qualifier: KClass<*>? = null,
        parameters: Parameters = emptyParameters()
    ): T = injektIntrinsic()
}

typealias BindingDefinition<T> = BindingDsl.(Parameters) -> T

/**
 * Registers the [binding] for [key]
 */
@Module
fun <T> addBinding(
    key: Key<T>,
    binding: Binding<T>
) {
    check(key !in componentDsl.bindings) {
        "Already declared binding for $key"
    }
    componentDsl.bindings[key] = binding
}
