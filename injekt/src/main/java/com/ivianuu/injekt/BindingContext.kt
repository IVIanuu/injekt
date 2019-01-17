package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Binding context
 */
data class BindingContext<T>(
    val binding: Binding<T>,
    val module: Module
)

/**
 * Binds this [Binding] to [type]
 */
infix fun <T> BindingContext<T>.bind(type: KClass<*>): BindingContext<T> {
    val copy = binding.copy(
        key = Key(type, name = null),
        type = type, name = null
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bind(types: Array<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bind(types: Iterable<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [Binding] to [name]
 */
infix fun <T> BindingContext<T>.bind(name: String): BindingContext<T> {
    val copy = binding.copy(
        key = Key(binding.key.type, name),
        name = name
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bind(names: Array<String>): BindingContext<T> = apply {
    names.forEach { bind(it) }
}

/**
 * Binds this [Binding] to [names]
 */
@JvmName("bindNames")
infix fun <T> BindingContext<T>.bind(names: Iterable<String>): BindingContext<T> = apply {
    names.forEach { bind(it) }
}