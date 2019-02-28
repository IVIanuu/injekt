package com.ivianuu.injekt

/**
 * Used for code gen
 */
interface BindingFactory<T> {
    fun create(): Module
}