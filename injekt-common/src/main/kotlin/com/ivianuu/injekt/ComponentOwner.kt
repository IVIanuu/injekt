package com.ivianuu.injekt

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

/**
 * @see Component.get
 */
@KeyOverload
fun <T> ComponentOwner.get(
    key: Key<T>,
    parameters: Parameters = emptyParameters()
): T = component.get(key, parameters)

/**
 * Lazy version of [get]
 *
 * @param key the key of the instance
 * @param parameters optional parameters to construct the instance
 * @return the instance

 * @see Component.get
 */
@KeyOverload
inline fun <T> ComponentOwner.getLazy(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): kotlin.Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { get(key, parameters()) }