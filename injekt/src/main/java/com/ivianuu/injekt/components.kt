package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Defines a component
 */
typealias ComponentDefinition = Component.() -> Unit

/**
 * Returns a new [Component] and applies the [definition]
 */
inline fun component(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component {
    return Component()
        .apply {
            definition.invoke(this)
            if (createEagerInstances) {
                createEagerInstances()
            }
        }
}


/**
 * Adds all [modules]
 */
fun Component.modules(modules: Iterable<Module>) {
    modules.forEach(this::addModule)
}

/**
 * Adds all [modules]
 */
fun Component.modules(vararg modules: Module) {
    modules.forEach(this::addModule)
}

/**
 * Adds the module
 */
fun Component.modules(module: Module) {
    addModule(module)
}

/**
 * Adds all [dependencies]
 */
fun Component.dependencies(dependencies: Iterable<Component>) {
    dependencies.forEach(this::addDependency)
}

/**
 * Adds all [dependencies]
 */
fun Component.dependencies(vararg dependencies: Component) {
    dependencies.forEach(this::addDependency)
}

/**
 * Adds the [dependency]
 */
fun Component.dependencies(dependency: Component) {
    addDependency(dependency)
}

/**
 * Adds all of [scopeNames]
 */
fun Component.scopeNames(scopeNames: Iterable<String>) {
    scopeNames.forEach(this::addScopeName)
}

/**
 * Adds all [scopeNames]
 */
fun Component.scopeNames(vararg scopeNames: String) {
    scopeNames.forEach(this::addScopeName)
}

/**
 * Adds the [scopeName]
 */
fun Component.scopeNames(scopeName: String) {
    addScopeName(scopeName)
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

fun Component.componentName(): String {
    return if (getScopeNames().isNotEmpty()) {
        "Component[${getScopeNames().joinToString(",")}]"
    } else {
        "Component[Unscoped]"
    }
}