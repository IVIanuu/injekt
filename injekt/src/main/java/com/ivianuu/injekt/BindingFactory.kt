package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Used for code generation
 */
interface BindingFactory<T> {
    fun create(): Binding<T>
}

internal object FactoryFinder {

    private val factories = hashMapOf<KClass<*>, BindingFactory<*>>()
    private val failedTypes = hashSetOf<KClass<*>>()

    fun <T> find(type: KClass<*>): BindingFactory<T>? {
        if (failedTypes.contains(type)) return null

        return try {
            val factoryName = type.java.name.replace("\$", "_") + "_Factory"
            val factoryType = Class.forName(factoryName)
            val factory = factoryType.newInstance() as BindingFactory<T>
            factories[type] = factory
            InjektPlugins.logger?.info("Found binding factory for $type")
            factory
        } catch (e: Exception) {
            failedTypes.add(type)
            null
        }
    }

}