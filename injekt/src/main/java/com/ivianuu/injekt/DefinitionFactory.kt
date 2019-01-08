package com.ivianuu.injekt

/**
 * Used for code generation
 */
interface DefinitionFactory<T : Any> {
    fun create(): BeanDefinition<T>
}