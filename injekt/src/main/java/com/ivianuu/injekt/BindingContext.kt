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
 * Adds this [Binding] to [type]
 */
infix fun <T> BindingContext<T>.bindType(type: KClass<*>): BindingContext<T> {
    val copy = binding.copy(
        key = Key(type),
        type = type,
        qualifier = null
    )
    module.add(copy)
    return this
}

/**
 * Adds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Array<KClass<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

/**
 * Adds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Iterable<KClass<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

/**
 * Adds this [Binding] to [qualifier]
 */
infix fun <T> BindingContext<T>.bindQualifier(qualifier: Qualifier): BindingContext<T> {
    val copy = binding.copy(
        key = Key(binding.type, qualifier),
        qualifier = qualifier
    )
    module.add(copy)
    return this
}

/**
 * Adds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindQualifiers(names: Array<out Qualifier>): BindingContext<T> {
    names.forEach { bindQualifier(it) }
    return this
}

/**
 * Adds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindQualifiers(names: Iterable<out Qualifier>): BindingContext<T> {
    names.forEach { bindQualifier(it) }
    return this
}