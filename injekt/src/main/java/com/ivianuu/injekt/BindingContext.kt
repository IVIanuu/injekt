package com.ivianuu.injekt

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