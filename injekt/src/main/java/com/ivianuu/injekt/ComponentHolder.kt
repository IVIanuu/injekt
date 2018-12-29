package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Holds a [Component] and allows for shorter syntax
 */
interface ComponentHolder {
    /**
     * The [Component] of this class
     */
    val component: Component
}

/** Calls trough [Component.get] */
inline fun <reified T : Any> ComponentHolder.get(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = component.get(T::class, name, params)

/** Calls trough [Component.getSet] */
inline fun <reified T : Any> ComponentHolder.getSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = component.getSet(T::class, setName, params)

/** Calls trough [Component.getSet] */
fun <T : Any> ComponentHolder.getSet(
    setType: KClass<T>,
    setName: String? = null,
    params: ParamsDefinition? = null
) = component.getSet(setType, setName, params)

/** Calls trough [Component.getProviderSet] */
inline fun <reified T : Any> ComponentHolder.getProviderSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = component.getProviderSet(T::class, setName, params)

/** Calls trough [Component.getProviderSet] */
fun <T : Any> ComponentHolder.getProviderSet(
    setType: KClass<T>,
    setName: String? = null,
    params: ParamsDefinition? = null
) = component.getProviderSet(setType, setName, params)

/** Calls trough [Component.getMap] */
inline fun <reified K : Any, reified T : Any> ComponentHolder.getMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = component.getMap<K, T>(T::class, mapName, params)

/** Calls trough [Component.getMap] */
fun <K : Any, T : Any> ComponentHolder.getMap(
    mapType: KClass<T>,
    mapName: String? = null,
    params: ParamsDefinition? = null
) = component.getMap<K, T>(mapType, mapName, params)

/** Calls trough [Component.getProviderMap] */
inline fun <reified K : Any, reified T : Any> ComponentHolder.getProviderMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = component.getProviderMap<K, T>(T::class, mapName, params)

/** Calls trough [Component.getMap] */
fun <K : Any, T : Any> ComponentHolder.getProviderMap(
    mapType: KClass<T>,
    mapName: String? = null,
    params: ParamsDefinition? = null
) = component.getProviderMap<K, T>(mapType, mapName, params)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> ComponentHolder.inject(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = inject(T::class, name, params)

/** Calls trough [Component.inject] */
fun <T : Any> ComponentHolder.inject(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = lazy { component.get(type, name, params) }

/** Calls trough [Component.provider] */
inline fun <reified T : Any> ComponentHolder.provider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = provider(T::class, name, defaultParams)

/** Calls trough [Component.provider] */
fun <T : Any> ComponentHolder.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = component.provider(type, name, defaultParams)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> ComponentHolder.injectProvider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = injectProvider(T::class, name, defaultParams)

/** Calls trough [Component.injectProvider] */
fun <T : Any> ComponentHolder.injectProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = lazy { component.provider(type, name, defaultParams) }

/** Calls trough [Component.injectSet] */
inline fun <reified T : Any> ComponentHolder.injectSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = lazy { component.getSet(T::class, setName, params) }

/** Calls trough [Component.injectSet] */
fun <T : Any> ComponentHolder.injectSet(
    setType: KClass<T>,
    setName: String? = null,
    params: ParamsDefinition? = null
) = lazy { component.getSet(setType, setName, params) }

/** Calls trough [Component.injectProviderSet] */
inline fun <reified T : Any> ComponentHolder.injectProviderSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = lazy { component.getProviderSet(T::class, setName, params) }

/** Calls trough [Component.injectProviderSet] */
fun <T : Any> ComponentHolder.injectProviderSet(
    setType: KClass<T>,
    setName: String? = null,
    params: ParamsDefinition? = null
) = lazy { component.getProviderSet(setType, setName, params) }

/** Calls trough [Component.injectMap] */
inline fun <reified K : Any, reified T : Any> ComponentHolder.injectMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = lazy { component.getMap<K, T>(T::class, mapName, params) }

/** Calls trough [Component.injectMap] */
fun <K : Any, T : Any> ComponentHolder.injectMap(
    mapType: KClass<T>,
    mapName: String? = null,
    params: ParamsDefinition? = null
) = lazy { component.getMap<K, T>(mapType, mapName, params) }

/** Calls trough [Component.injectProviderMap] */
inline fun <reified K : Any, reified T : Any> ComponentHolder.injectProviderMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = lazy { component.getProviderMap<K, T>(T::class, mapName, params) }

/** Calls trough [Component.injectProviderMap] */
fun <K : Any, T : Any> ComponentHolder.injectProviderMap(
    mapType: KClass<T>,
    mapName: String? = null,
    params: ParamsDefinition? = null
) = lazy { component.getProviderMap<K, T>(mapType, mapName, params) }