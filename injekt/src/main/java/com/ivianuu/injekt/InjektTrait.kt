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
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = component.get(qualifier, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T> InjektTrait.inject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy { component.get<T>(qualifier, parameters) }

/** Calls trough [Component.getProvider] */
inline fun <reified T> InjektTrait.getProvider(
    qualifier: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = component.getProvider(qualifier, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T> InjektTrait.injectProvider(
    qualifier: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazy { component.getProvider<T>(qualifier, defaultParameters) }