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
    noinline params: ParamsDefinition? = null
) = component.get(T::class, name, params)

/** Calls trough [Component.get] */
fun <T : Any> InjektTrait.get(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = component.get(type, name, params)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> InjektTrait.inject(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = inject(T::class, name, params)

/** Calls trough [Component.inject] */
fun <T : Any> InjektTrait.inject(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = lazy { component.get(type, name, params) }

/** Calls trough [Component.provider] */
inline fun <reified T : Any> InjektTrait.provider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = provider(T::class, name, defaultParams)

/** Calls trough [Component.provider] */
fun <T : Any> InjektTrait.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = component.provider(type, name, defaultParams)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> InjektTrait.injectProvider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = injectProvider(T::class, name, defaultParams)

/** Calls trough [Component.injectProvider] */
fun <T : Any> InjektTrait.injectProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = lazy { component.provider(type, name, defaultParams) }