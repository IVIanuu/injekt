package com.ivianuu.injekt

/**
 * Holds a [Component] and allows for shorter syntax
 */
interface InjektTrait {
    /**
     * The [Component] of this class
     */
    val component: Component
}

/** Calls trough [Component.get] */
inline fun <reified T> InjektTrait.get(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): T = component.get(T::class, name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T> InjektTrait.inject(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = component.inject(name, parameters)

/** Calls trough [Component.getProvider] */
inline fun <reified T> InjektTrait.getProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = component.getProvider(name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T> InjektTrait.injectProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = component.injectProvider(name, defaultParameters)