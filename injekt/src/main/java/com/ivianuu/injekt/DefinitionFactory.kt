package com.ivianuu.injekt

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface DefinitionFactory<T : Any> {
    fun create(): BeanDefinition<T>
}