package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger
import kotlin.reflect.KClass

/**
 * The actual dependency container which provides definitions
 */
class Component internal constructor(
    val scopeId: String,
    val name: String?
) {

    val beanRegistry = BeanRegistry(this)

    /**
     * Adds all [BeanDefinition]s of the [modules]
     */
    fun modules(modules: Iterable<Module>) {
        beanRegistry.loadModules(modules)
    }

    /**
     * Adds all current [BeanDefinition]s of [components] to this component
     * The instances of the definitions will still live in the owning component
     */
    fun components(components: Iterable<Component>) {
        beanRegistry.linkComponents(components)
    }

    /**
     * Instantiates all eager instances
     */
    fun createEagerInstances() {
        logger?.info("$scopeId Create start up instances")
        beanRegistry.getEagerInstances().forEach { it.resolveInstance() }
    }

    /**
     * Returns a instance of [T] matching the [type], [name] and [params]
     */
    fun <T : Any> get(
        type: KClass<T>,
        name: String? = null,
        params: ParamsDefinition? = null
    ): T {
        val definition = beanRegistry.findDefinition(type, name)

        return if (definition != null) {
            @Suppress("UNCHECKED_CAST")
            definition.resolveInstance(params) as T
        } else {
            throw NoBeanDefinitionFoundException("${this.name} Could not find definition for ${type.java.name + " " + name.orEmpty()}")
        }
    }

}

/**
 * Defines a component
 */
typealias ComponentDefinition = Component.() -> Unit

/**
 * Returns a new [Component] and applies the [definition]
 */
fun component(
    scopeId: String,
    name: String? = null,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition
) = Component(scopeId, name)
    .apply(definition)
    .apply {
        if (createEagerInstances) {
            createEagerInstances()
        }
    }

/** Calls trough [Component.modules] */
fun Component.modules(vararg modules: Module) {
    modules(modules.asIterable())
}

/** Calls trough [Component.components] */
fun Component.components(vararg components: Component) {
    components(components.asIterable())
}

/**
 * Adds a [BeanDefinition] for the [instance]
 */
fun <T : Any> Component.addInstance(instance: T) {
    beanRegistry.saveDefinition(
        BeanDefinition.createSingle(
            instance::class as KClass<T>,
            null
        ) { instance }
    )
}

/**
 * Returns a instance of [T] matching the [name] and [params]
 */
inline fun <reified T : Any> Component.get(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = get(T::class, name, params)

/**
 * Lazily returns a instance of [T] matching the [name] and [params]
 */
inline fun <reified T : Any> Component.inject(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = inject(T::class, name, params)

/**
 * Lazily returns a instance of [T] matching the [name] and [params]
 */
fun <T : Any> Component.inject(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = lazy { get(type, name, params) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T : Any> Component.provider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = provider(T::class, name, defaultParams)

/**
 * Returns a [Provider] for [type] and [name]
 * Each [Provider.get] results in a potentially new value
 */
fun <T : Any> Component.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = provider { params: ParamsDefinition? -> get(type, name, params ?: defaultParams) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T : Any> Component.injectProvider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = injectProvider(T::class, name, defaultParams)

/**
 * Returns a [Provider] for [type] and [name]
 * Each [Provider.get] results in a potentially new value
 */
fun <T : Any> Component.injectProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = lazy { provider { params: ParamsDefinition? -> get(type, name, params ?: defaultParams) } }