package com.ivianuu.injekt

import kotlin.reflect.KClass

object CompositionFactories {

    private val factories = mutableMapOf<KClass<*>, Any>()

    fun register(component: KClass<*>, factory: Any) {
        factories[component] = factory
    }

    fun <T> get(component: KClass<*>): T {
        return factories[component] as? T
            ?: error("Couldn't get factory for component ${component.java.name}")
    }
}
