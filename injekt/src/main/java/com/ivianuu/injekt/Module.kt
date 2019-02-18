package com.ivianuu.injekt

import java.util.*
import kotlin.reflect.KClass

/**
 * A module is the container for bindings
 */
class Module internal constructor(
    val name: String?,
    val scopeName: String?,
    val eager: Boolean?,
    val override: Boolean?
) {

    internal val bindings = mutableMapOf<Key, Binding<*>>()

    /**
     * Returns all [Binding]s of this module
     */
    fun getBindings(): Set<Binding<*>> = bindings.values.toSet()

    /**
     * Adds the [binding]
     */
    fun <T> declare(
        binding: Binding<T>
    ): BindingContext<T> {
        var binding = binding
        val scopeName = scopeName ?: binding.scopeName
        val override = override ?: binding.override
        val eager = eager ?: binding.eager

        if (binding.scopeName != scopeName
            || binding.eager != eager
            || binding.override != override
        ) {
            binding = binding.copy(
                scopeName = scopeName,
                eager = eager,
                override = override
            )
        }

        if (bindings.containsKey(binding.key) && !binding.override) {
            throw OverrideException("Try to override binding $binding but was already declared in $name")
        }

        bindings[binding.key] = binding

        return BindingContext(binding, this)
    }

}

/**
 * Defines module entries
 */
typealias ModuleDefinition = Module.() -> Unit

/**
 * Defines a [Module]
 */
fun module(
    name: String? = null,
    scopeName: String? = null,
    override: Boolean? = null,
    eager: Boolean? = null,
    definition: ModuleDefinition
): Module = Module(name, scopeName, eager, override).apply(definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T> Module.factory(
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = factory(T::class, name, scopeName, override, definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
fun <T> Module.factory(
    type: KClass<*>,
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    Binding.createFactory(
        type = type,
        name = name,
        scopeName = scopeName,
        override = override,
        definition = definition
    )
)

/**
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T> Module.single(
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = single(T::class, name, scopeName, override, eager, definition)

/**
 * Provides scoped dependency which will be created once for each component
 */
fun <T> Module.single(
    type: KClass<*>,
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    Binding.createSingle(
        type = type,
        name = name,
        scopeName = scopeName,
        override = override,
        eager = eager,
        definition = definition
    )
)

/**
 * Adds all bindings of [module]
 */
fun Module.module(module: Module) {
    module.bindings.forEach { declare(it.value) }
}

/**
 * Adds all bindings of module
 */
fun Module.module(
    name: String? = null,
    scopeName: String? = null,
    override: Boolean? = null,
    eager: Boolean? = null,
    definition: ModuleDefinition
) {
    module(com.ivianuu.injekt.module(name, scopeName, override, eager, definition))
}

/** Calls trough [Module.withBinding] */
inline fun <reified T> Module.withBinding(
    name: String? = null,
    body: BindingContext<T>.() -> Unit
) = withBinding(T::class, name, body)

/**
 * Invokes the [body] in the [BindingContext] of the [Binding] with [type] and [name]
 */
inline fun <T> Module.withBinding(
    type: KClass<*>,
    name: String? = null,
    body: BindingContext<T>.() -> Unit
) {
    // we create a additional binding because we have now reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // the new factory acts as bridge and just calls trough the original implementation
    factory<T>(type, UUID.randomUUID().toString()) { get(type, name) { it } } withContext body
}

operator fun Module.plus(module: Module): List<Module> = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>): List<Module> = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>): List<Module> = listOf(this) + modules