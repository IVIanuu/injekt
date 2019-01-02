package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger
import kotlin.reflect.KClass

/**
 * The actual dependency container which provides declarations
 */
class Component internal constructor(val name: String? = null) {

    val context = ComponentContext(this)
    val componentRegistry = ComponentRegistry(this)
    val declarationRegistry = DeclarationRegistry(this)

    init {
        logger?.info("$name created")
    }

    /**
     * Adds all [Declaration]s of the [module]
     */
    fun modules(vararg modules: Module) {
        declarationRegistry.loadModules(*modules)
    }

    /**
     * Adds all [Declaration]s of [components] to this component
     */
    fun dependencies(vararg components: Component) {
        componentRegistry.addComponents(*components)
    }

    /**
     * Instantiates all eager instances
     */
    fun createEagerInstances() {
        declarationRegistry.getEagerInstances().forEach {
            logger?.info("$name Create instance on start up $it")
            it.resolveInstance()
        }
    }

    /**
     * Returns a instance of [T] matching the [type], [name] and [params]
     */
    fun <T : Any> get(
        type: KClass<T>,
        name: String? = null,
        params: ParamsDefinition? = null
    ) = synchronized(this) {
        val key = Key.of(type, name)
        val declaration = findDeclaration(key)

        if (declaration != null) {
            @Suppress("UNCHECKED_CAST")
            logger?.let { logger ->
                logger.info(
                    if (declaration.instance.isCreated) {
                        "$name Return existing instance $declaration"
                    } else {
                        "$name Create instance $declaration"
                    }
                )
            }
            declaration.resolveInstance(params) as T
        } else {
            throw InjectionException("$name Could not find declaration for ${type.java.name + " " + name.orEmpty()}")
        }
    }

    private fun findDeclaration(key: Key): Declaration<*>? =
        declarationRegistry.findDeclaration(key)
            ?: componentRegistry.getDependencies().firstNotNull { it.findDeclaration(key) }

    private inline fun <T, R> Iterable<T>.firstNotNull(predicate: (T) -> R): R? {
        for (element in this) {
            val result = predicate(element)
            if (result != null) return result
        }
        return null
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
 * Adds all [Declaration]s of [components] to this component
 */
fun Component.dependencies(components: Collection<Component>) {
    dependencies(*components.toTypedArray())
}

/**
 * Returns a instance of [T] matching the [name] and [params]
 */
inline fun <reified T : Any> Component.get(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = get(T::class, name, params)

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
) = provider { params: ParamsDefinition? -> get(type, name, params ?: defaultParams) }

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
) = lazy { provider { params: ParamsDefinition? -> get(type, name, params ?: defaultParams) } }