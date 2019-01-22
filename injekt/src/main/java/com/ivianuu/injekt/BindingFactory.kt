package com.ivianuu.injekt

/**
 * Used for code generation
 */
interface BindingFactory<T> {
    fun create(): Binding<T>
}