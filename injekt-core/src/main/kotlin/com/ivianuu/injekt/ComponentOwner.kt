package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Holds a [Component] and allows for shorter syntax and lazy construction of a component
 *
 * Example:
 *
 * ```
 * class MainActivity : Activity(), ComponentOwner {
 *
 *     override val component = Component { ... }
 *
 *     private val dep1: Dependency1 by getLazy()
 *     private val dep2: Dependency2 by getLazy()
 *
 * }
 * ```
 *
 */
interface ComponentOwner {
    /**
     * The [Component] which will be used to retrieve instances
     */
    val component: Component
}

inline fun <reified T> ComponentOwner.get(
    qualifier: KClass<*>? = null,
    parameters: Parameters = emptyParameters()
): T = get(keyOf(qualifier), parameters)

/**
 * @see Component.get
 */
fun <T> ComponentOwner.get(
    key: Key<T>,
    parameters: Parameters = emptyParameters()
): T = component.get(key, parameters)

inline fun <reified T> ComponentOwner.getLazy(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): kotlin.Lazy<T> = getLazy(keyOf(qualifier), parameters)

/**
 * Lazy version of [get]
 */
inline fun <T> ComponentOwner.getLazy(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): kotlin.Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(key, parameters()) }
