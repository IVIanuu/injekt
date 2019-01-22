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
 * Invokes the [body]
 */
inline infix fun <T> BindingContext<T>.withContext(body: BindingContext<T>.() -> Unit): BindingContext<T> {
    body()
    return this
}

/**
 * Binds this [Binding] to [type]
 */
infix fun <T> BindingContext<T>.bindType(type: KClass<*>): BindingContext<T> {
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
infix fun <T> BindingContext<T>.bindTypes(types: Array<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bindType(it) }
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Iterable<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bindType(it) }
}

/**
 * Binds this [Binding] to [name]
 */
infix fun <T> BindingContext<T>.bindName(name: String): BindingContext<T> {
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
infix fun <T> BindingContext<T>.bindNames(names: Array<String>): BindingContext<T> = apply {
    names.forEach { bindName(it) }
}

/**
 * Binds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindNames(names: Iterable<String>): BindingContext<T> = apply {
    names.forEach { bindName(it) }
}