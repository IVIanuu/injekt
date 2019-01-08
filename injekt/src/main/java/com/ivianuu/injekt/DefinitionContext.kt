package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Environment for [Definition]s
 */
class DefinitionContext(val component: Component)

/** Calls trough [Component.get] */
inline fun <reified T : Any> DefinitionContext.get(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/** Calls trough [Component.get] */
fun <T : Any> DefinitionContext.get(
    type: KClass<T>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): T = component.get(type, name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> DefinitionContext.inject(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/** Calls trough [Component.inject] */
fun <T : Any> DefinitionContext.inject(
    type: KClass<T>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy { get(type, name, parameters) }

/** Calls trough [Component.getProvider] */
inline fun <reified T : Any> DefinitionContext.getProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = getProvider(T::class, name, defaultParameters)

/** Calls trough [Component.getProvider] */
fun <T : Any> DefinitionContext.getProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = component.getProvider(type, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> DefinitionContext.lazyProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazyProvider(T::class, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
fun <T : Any> DefinitionContext.lazyProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = component.injectProvider(type, name, defaultParameters)