package com.ivianuu.injekt

import com.ivianuu.injekt.Declaration.Kind
import kotlin.reflect.KClass

/**
 * A module provides the actual dependencies
 */
class Module internal constructor(
    val name: String? = null,
    val createOnStart: Boolean,
    val override: Boolean
) {

    internal val declarations = hashMapOf<Key, Declaration<*>>()

    /**
     * Adds the [declaration]
     */
    fun <T : Any> declare(
        declaration: Declaration<T>
    ): Declaration<T> {
        val oldDeclaration = declarations[declaration.key]
        val isOverride = oldDeclaration != null
        if (isOverride && !declaration.override) {
            throw OverrideException("$name Try to override declaration $declaration but was already saved $oldDeclaration")
        }

        declaration.module = this

        val createOnStart = if (createOnStart) createOnStart else declaration.createOnStart
        val override = if (override) override else declaration.override

        declaration.createOnStart = createOnStart
        declaration.override = override

        declarations[declaration.key] = declaration

        return declaration
    }

}

/**
 * Defines a [Module]
 */
fun module(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: ModuleDefinition
) = Module(name, createOnStart, override).apply(definition)

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
    name: String? = null,
    kind: Kind,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
) = declare(T::class, name, kind, override, createOnStart, definition)

/**
 * Adds a [Declaration] for the provided params
 */
fun <T : Any> Module.declare(
    type: KClass<T>,
    name: String? = null,
    kind: Kind,
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
 * Adds all declarations of [module]
 */
fun Module.module(module: Module) {
    module.declarations.forEach { declare(it.value.clone()) }
}

/**
 * Adds all declarations of module
 */
fun Module.module(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: ModuleDefinition
) {
    module(com.ivianuu.injekt.module(name, override, createOnStart, definition))
}

/**
 * Adds a binding for [T] for a existing declaration of [S]
 */
inline fun <reified T : Any, reified S : T> Module.bind(name: String? = null) =
    factory<T>(name) { get<S>() }