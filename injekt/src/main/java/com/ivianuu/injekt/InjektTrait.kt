package com.ivianuu.injekt

import kotlin.reflect.KClass

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
inline fun <reified T : Any> InjektTrait.get(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): T = component.get(T::class, name, parameters)

/** Calls trough [Component.get] */
fun <T : Any> InjektTrait.get(
    type: KClass<T>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): T = component.get(type, name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> InjektTrait.inject(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/** Calls trough [Component.inject] */
fun <T : Any> InjektTrait.inject(
    type: KClass<T>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy { component.get(type, name, parameters) }

/** Calls trough [Component.provider] */
inline fun <reified T : Any> InjektTrait.provider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = provider(T::class, name, defaultParameters)

/** Calls trough [Component.provider] */
fun <T : Any> InjektTrait.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = component.provider(type, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> InjektTrait.injectProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = injectProvider(T::class, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
fun <T : Any> InjektTrait.injectProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazy { component.provider(type, name, defaultParameters) }