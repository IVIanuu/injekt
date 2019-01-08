package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Used for code generation
 */
interface DefinitionFactory<T : Any> {
    fun create(): BeanDefinition<T>
}

internal object FactoryFinder {

    private val factories = hashMapOf<KClass<*>, DefinitionFactory<*>>()
    private val failedTypes = hashSetOf<KClass<*>>()

    fun <T : Any> find(type: KClass<T>): DefinitionFactory<T>? {
        if (failedTypes.contains(type)) return null
        return try {
            val factoryName = type.java.name.replace("\$", "_") + "__Factory"
            val factoryType = Class.forName(factoryName)
            val factory = factoryType.newInstance() as DefinitionFactory<T>
            factories[type] = factory
            InjektPlugins.logger?.info("Found definition factory for $type")
            factory
        } catch (e: Exception) {
            failedTypes.add(type)
            null
        }
    }

}