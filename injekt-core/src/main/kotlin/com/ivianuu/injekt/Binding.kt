package com.ivianuu.injekt

import kotlin.reflect.KClass

sealed class Binding<T> {
    abstract fun link(linker: Linker): LinkedBinding<T>
}

abstract class UnlinkedBinding<T> : Binding<T>()

abstract class LinkedBinding<T> : Binding<T>(), Provider<T> {
    final override fun link(linker: Linker): LinkedBinding<T> = this
}

class BindingDsl {
    fun <T> get(key: Key<T>): T = error("Implemented as an intrinsic")
    inline fun <reified T> get(qualifier: KClass<*>? = null): T = get(keyOf(qualifier))
}

typealias BindingDefinition<T> = BindingDsl.(Parameters) -> T
