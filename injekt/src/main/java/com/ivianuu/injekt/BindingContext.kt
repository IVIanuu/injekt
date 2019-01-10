package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Binding context
 */
data class BindingContext<T : Any>(
    val binding: Binding<T>,
    val module: Module
)

/**
 * Binds this [Binding] to [type]
 */
infix fun <T : Any> BindingContext<T>.bind(type: KClass<*>): BindingContext<T> {
    val copy = (binding as Binding<Any>).copy(
        key = Key(type, name = null),
        type = type as KClass<Any>, name = null
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T : Any> BindingContext<T>.bind(types: Array<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T : Any> BindingContext<T>.bind(types: Iterable<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [Binding] to [name]
 */
infix fun <T : Any> BindingContext<T>.bind(name: String): BindingContext<T> {
    val copy = (binding as Binding<Any>).copy(
        key = Key(binding.key.type, name),
        name = name
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [Binding] to [names]
 */
infix fun <T : Any> BindingContext<T>.bind(names: Array<String>): BindingContext<T> = apply {
    names.forEach { bind(it) }
}

/**
 * Binds this [Binding] to [names]
 */
@JvmName("bindNames")
infix fun <T : Any> BindingContext<T>.bind(names: Iterable<String>): BindingContext<T> = apply {
    names.forEach { bind(it) }
}