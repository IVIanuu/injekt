package com.ivianuu.injekt

import com.ivianuu.injekt.Declaration.Kind
import kotlin.reflect.KClass

/**
 * A module provides the actual dependencies
 */
class Module internal constructor(
    val createOnStart: Boolean,
    val override: Boolean,
    val name: String?
) {

    lateinit var component: Component
        internal set

    internal val declarations = arrayListOf<Declaration<*>>()

    /**
     * Adds the [declaration]
     */
    fun <T : Any> declare(
        declaration: Declaration<T>
    ): Declaration<T> {
        declaration.module = this

        val createOnStart = if (createOnStart) createOnStart else declaration.createOnStart
        val override = if (override) override else declaration.override

        declaration.createOnStart = createOnStart
        declaration.override = override

        declarations.add(declaration)

        return declaration
    }

}

/**
 * Defines a [Module]
 */
fun module(
    createOnStart: Boolean = false,
    override: Boolean = false,
    name: String? = null,
    definition: ModuleDefinition
) = Module(createOnStart, override, name).apply(definition)

/**
 * Provides a dependency
 */
inline fun <reified T : Any> Module.factory(
    name: String? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) = factory(T::class, name, override, definition)

/**
 * Provides a dependency
 */
fun <T : Any> Module.factory(
    type: KClass<T>,
    name: String? = null,
    override: Boolean = false,
    definition: Definition<T>
) = declare(
    type = type,
    kind = Kind.FACTORY,
    name = name,
    createOnStart = false,
    override = override,
    definition = definition
)

/**
 * Provides a singleton dependency
 */
inline fun <reified T : Any> Module.single(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
) = single(T::class, name, override, createOnStart, definition)

/**
 * Provides a singleton dependency
 */
fun <T : Any> Module.single(
    type: KClass<T>,
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: Definition<T>
) = declare(
    type = type,
    kind = Kind.SINGLE,
    name = name,
    override = override,
    createOnStart = createOnStart,
    definition = definition
)

/**
 * Adds a [Declaration] for the provided params
 */
inline fun <reified T : Any> Module.declare(
    kind: Kind,
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
) = declare(
    Declaration.create(T::class, name, kind, definition).also {
        it.createOnStart = createOnStart
        it.override = override
    }
)

/**
 * Adds a [Declaration] for the provided params
 */
fun <T : Any> Module.declare(
    type: KClass<T>,
    kind: Kind,
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: Definition<T>
) = declare(
    Declaration.create(type, name, kind, definition).also {
        it.createOnStart = createOnStart
        it.override = override
    }
)

/**
 * Adds a binding for [T] for a existing declaration of [S]
 */
inline fun <reified T : Any, reified S : T> Module.bind() =
    factory<T> { get<S>() }

/** Calls trough [Component.get] */
inline fun <reified T : Any> Module.get(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = get(T::class, name, params)

/** Calls trough [Component.get] */
fun <T : Any> Module.get(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = component.get(type, name, params)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> Module.lazy(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = lazy(T::class, name, params)

/** Calls trough [Component.inject] */
fun <T : Any> Module.lazy(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
): Lazy<T> = kotlin.lazy { get(type, name, params) }

/** Calls trough [Component.provider] */
inline fun <reified T : Any> Module.provider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = provider(T::class, name, defaultParams)

/** Calls trough [Component.provider] */
fun <T : Any> Module.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = component.provider(type, name, defaultParams)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> Module.lazyProvider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = lazyProvider(T::class, name, defaultParams)

/** Calls trough [Component.injectProvider] */
fun <T : Any> Module.lazyProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = component.injectProvider(type, name, defaultParams)