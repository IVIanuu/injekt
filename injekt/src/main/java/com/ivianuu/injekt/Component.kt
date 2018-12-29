package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * The actual dependency container which provides declarations
 */
class Component internal constructor(val name: String?) {

    val declarationRegistry = DeclarationRegistry(name, this)

    /**
     * Adds all [Declaration]s of the [module]
     */
    fun modules(vararg modules: Module) {
        declarationRegistry.loadModules(*modules)
    }

    /**
     * Adds all [Declaration]s of [dependencies] to this component
     */
    fun dependencies(vararg dependencies: Component) {
        declarationRegistry.loadDependencies(*dependencies)
    }

    /**
     * Instantiates all eager instances
     */
    fun createEagerInstances() {
        declarationRegistry.getEagerInstances().forEach { it.resolveInstance(null) }
    }

    /**
     * Returns a instance of [T] matching the [type], [name] and [params]
     */
    fun <T : Any> get(
        type: KClass<T>,
        name: String? = null,
        params: ParamsDefinition? = null
    ) = synchronized(this) {
        val declaration = declarationRegistry.findDeclaration(type, name)

        if (declaration != null) {
            @Suppress("UNCHECKED_CAST")
            declaration.resolveInstance(params) as T
        } else {
            throw InjectionException("${nameString()}Could not find declaration for ${type.java.name + name.orEmpty()}")
        }
    }

    /**
     * Returns a [Set] of [T] matching the [setType] and [setName]
     */
    fun <T : Any> getSet(
        setType: KClass<T>,
        setName: String? = null,
        params: ParamsDefinition? = null
    ): Set<T> = synchronized(this) {
        declarationRegistry.getSetDeclarations(setType, setName)
            .map { it.resolveInstance(params) }
            .toSet() as Set<T>
    }

    /**
     * Returns a [Set] of [T] matching the [setType] and [setName]
     */
    fun <T : Any> getProviderSet(
        setType: KClass<T>,
        setName: String? = null,
        defaultParams: ParamsDefinition? = null
    ): Set<Provider<T>> = synchronized(this) {
        declarationRegistry.getSetDeclarations(setType, setName)
            .map { declaration ->
                Provider { declaration.resolveInstance(it ?: defaultParams) as T }
            }
            .toSet()
    }

    /**
     * Returns a [Map] of [K],[T] matching the [mapType] and [mapName]
     */
    fun <K : Any, T : Any> getMap(
        mapType: KClass<T>,
        mapName: String? = null,
        params: ParamsDefinition? = null
    ) = synchronized(this) {
        declarationRegistry.getMapDeclarations(mapType, mapName)
            .mapKeys { it.key as K }
            .mapValues { it.value.resolveInstance(params) as T }
    }

    /**
     * Returns a [Map] of [K],[T] matching the [mapType] and [mapName]
     */
    fun <K : Any, T : Any> getProviderMap(
        mapType: KClass<T>,
        mapName: String? = null,
        defaultParams: ParamsDefinition? = null
    ): Map<K, Provider<T>> = synchronized(this) {
        declarationRegistry.getMapDeclarations(mapType, mapName)
            .mapKeys { it.key as K }
            .mapValues { (_, declaration) ->
                Provider { declaration.resolveInstance(it ?: defaultParams) as T }
            }
    }

}

/**
 * Returns a new [Component] and applies the [definition]
 */
fun component(
    name: String? = null,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition
) = Component(name)
    .apply(definition)
    .apply {
        if (createEagerInstances) {
            createEagerInstances()
        }
    }

/**
 * Adds all [Declaration]s of the [module]
 */
fun Component.modules(modules: Collection<Module>) {
    modules(*modules.toTypedArray())
}

/**
 * Adds all [Declaration]s of [dependencies] to this component
 */
fun Component.dependencies(dependencies: Collection<Component>) {
    declarationRegistry.loadDependencies(*dependencies.toTypedArray())
}

/**
 * Returns a instance of [T] matching the [name] and [params]
 */
inline fun <reified T : Any> Component.get(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = get(T::class, name, params)

/**
 * Returns a [Set] of [T] matching [T] and [setName]
 */
inline fun <reified T : Any> Component.getSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = getSet(T::class, setName, params)

/**
 * Returns a [Set] of [T] matching [T] and [setName]
 */
inline fun <reified T : Any> Component.getProviderSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = getProviderSet(T::class, setName, params)

/**
 * Returns a [Map] of [K],[T] matching the [T] and [mapName]
 */
inline fun <reified K : Any, reified T : Any> Component.getMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = getMap<K, T>(T::class, mapName, params)

/**
 * Returns a [Map] of [K],[T] matching the [T] and [mapName]
 */
inline fun <reified K : Any, reified T : Any> Component.getProviderMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = getProviderMap<K, T>(T::class, mapName, params)

/**
 * Lazily returns a instance of [T] matching the [name] and [params]
 */
inline fun <reified T : Any> Component.inject(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = inject(T::class, name, params)

/**
 * Lazily returns a instance of [T] matching the [name] and [params]
 */
fun <T : Any> Component.inject(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = lazy { get(type, name, params) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T : Any> Component.provider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = provider(T::class, name, defaultParams)

/**
 * Returns a [Provider] for [type] and [name]
 * Each [Provider.get] results in a potentially new value
 */
fun <T : Any> Component.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = Provider { get(type, name, it ?: defaultParams) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T : Any> Component.injectProvider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = injectProvider(T::class, name, defaultParams)

/**
 * Returns a [Provider] for [type] and [name]
 * Each [Provider.get] results in a potentially new value
 */
fun <T : Any> Component.injectProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = lazy { Provider { get(type, name, it ?: defaultParams) } }

/**
 * Returns a [Set] of [T] matching [T] and [setName]
 */
inline fun <reified T : Any> Component.injectSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = injectSet(T::class, setName, params)

/**
 * Returns a [Set] of [T] matching the [setType] and [setName]
 */
fun <T : Any> Component.injectSet(
    setType: KClass<T>,
    setName: String? = null,
    params: ParamsDefinition? = null
) = lazy { getSet(setType, setName, params) }

/**
 * Returns a [Set] of [T] matching [T] and [setName]
 */
inline fun <reified T : Any> Component.injectProviderSet(
    setName: String? = null,
    noinline params: ParamsDefinition? = null
) = injectProviderSet(T::class, setName, params)

/**
 * Returns a [Set] of [T] matching the [setType] and [setName]
 */
fun <T : Any> Component.injectProviderSet(
    setType: KClass<T>,
    setName: String? = null,
    params: ParamsDefinition? = null
) = lazy { getProviderSet(setType, setName, params) }

/**
 * Returns a [Map] of [K],[T] matching the [T] and [mapName]
 */
inline fun <reified K : Any, reified T : Any> Component.injectMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = injectMap<K, T>(T::class, mapName, params)

/**
 * Returns a [Map] of [K],[T] matching the [mapType] and [mapName]
 */
fun <K : Any, T : Any> Component.injectMap(
    mapType: KClass<T>,
    mapName: String? = null,
    params: ParamsDefinition? = null
) = lazy { getMap<K, T>(mapType, mapName, params) }

/**
 * Returns a [Map] of [K],[T] matching the [T] and [mapName]
 */
inline fun <reified K : Any, reified T : Any> Component.injectProviderMap(
    mapName: String? = null,
    noinline params: ParamsDefinition? = null
) = injectProviderMap<K, T>(T::class, mapName, params)

/**
 * Returns a [Map] of [K],[T] matching the [mapType] and [mapName]
 */
fun <K : Any, T : Any> Component.injectProviderMap(
    mapType: KClass<T>,
    mapName: String? = null,
    params: ParamsDefinition? = null
) = lazy { getProviderMap<K, T>(mapType, mapName, params) }