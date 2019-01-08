package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Binding context
 */
data class BindingContext<T : Any>(
    val definition: BeanDefinition<T>,
    val module: Module
)

/**
 * Binds this [BeanDefinition] to [type]
 */
infix fun <T : Any> BindingContext<T>.bind(type: KClass<*>): BindingContext<T> {
    val copy = (definition as BeanDefinition<Any>).copy(
        key = Key(type, name = null),
        type = type as KClass<Any>, name = null
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [BeanDefinition] to [types]
 */
infix fun <T : Any> BindingContext<T>.bind(types: Array<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [BeanDefinition] to [types]
 */
infix fun <T : Any> BindingContext<T>.bind(types: Iterable<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [BeanDefinition] to [name]
 */
infix fun <T : Any> BindingContext<T>.bind(name: String): BindingContext<T> {
    val copy = (definition as BeanDefinition<Any>).copy(
        key = Key(definition.key.type, name),
        name = name
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [BeanDefinition] to [names]
 */
infix fun <T : Any> BindingContext<T>.bind(names: Array<String>): BindingContext<T> = apply {
    names.forEach { bind(it) }
}

/**
 * Binds this [BeanDefinition] to [names]
 */
@JvmName("bindNames")
infix fun <T : Any> BindingContext<T>.bind(names: Iterable<String>): BindingContext<T> = apply {
    names.forEach { bind(it) }
}