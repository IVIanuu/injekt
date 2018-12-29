package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * The actual dependency container which provides declarations
 */
class Component internal constructor(val name: String?) {

    val declarationRegistry = DeclarationRegistry(name).apply {
        setComponent(this@Component)
    }

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