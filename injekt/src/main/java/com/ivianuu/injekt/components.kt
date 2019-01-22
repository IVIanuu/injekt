package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Defines a component
 */
typealias ComponentDefinition = Component.() -> Unit

/**
 * Returns a new [Component] and applies the [definition]
 */
fun component(
    name: String? = null,
    deferCreateEagerInstances: Boolean = false,
    definition: ComponentDefinition? = null
): Component = Component(name)
    .apply {
        definition?.invoke(this)
        if (!deferCreateEagerInstances) {
            createEagerInstances()
        }
    }

/** Calls trough [Component.modules] */
fun Component.modules(vararg modules: Module) {
    modules(modules.asIterable())
}

/** Calls trough [Component.dependencies] */
fun Component.dependencies(vararg components: Component) {
    dependencies(components.asIterable())
}

/** Calls trough [Component.scopeNames] */
fun Component.scopeNames(vararg scopeNames: String) {
    scopeNames(scopeNames.asIterable())
}

/**
 * Adds a [Binding] for the [instance]
 */
fun <T : Any> Component.addInstance(instance: T) {
    addBinding(
        Binding.createSingle(
            instance::class,
            null
        ) { instance }
    )
}

/**
 * Returns a instance of [T] matching the [name] and [parameters]
 */
inline fun <reified T> Component.get(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/**
 * Lazily returns a instance of [T] matching the [name] and [parameters]
 */
inline fun <reified T> Component.inject(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/**
 * Lazily returns a instance of [T] matching the [name] and [parameters]
 */
fun <T> Component.inject(
    type: KClass<*>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy { get<T>(type, name, parameters) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.getProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = getProvider(T::class, name, defaultParameters)

/**
 * Returns a [Provider] for [type] and [name]
 * Each [Provider.get] results in a potentially new value
 */
fun <T> Component.getProvider(
    type: KClass<*>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = provider { parameters: ParametersDefinition? ->
    get<T>(
        type,
        name,
        parameters ?: defaultParameters
    )
}

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.injectProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = injectProvider(T::class, name, defaultParameters)

/**
 * Returns a [Provider] for [type] and [name]
 * Each [Provider.get] results in a potentially new value
 */
fun <T> Component.injectProvider(
    type: KClass<*>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazy {
    provider { parameters: ParametersDefinition? ->
        get<T>(
            type,
            name,
            parameters ?: defaultParameters
        )
    }
}