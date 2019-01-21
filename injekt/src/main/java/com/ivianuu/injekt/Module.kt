package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * A module is the container for bindings
 */
class Module internal constructor(
    val name: String?,
    val scopeName: String?,
    val eager: Boolean,
    val override: Boolean
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
        val override = if (override) override else binding.override
        val eager = if (eager) eager else binding.eager

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
    override: Boolean = false,
    eager: Boolean = false,
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
    type = type,
    name = name,
    kind = Binding.Kind.FACTORY,
    scopeName = scopeName,
    eager = false,
    override = override,
    definition = definition
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
    type = type,
    name = name,
    kind = Binding.Kind.SINGLE,
    scopeName = scopeName,
    override = override,
    eager = eager,
    definition = definition
)

/**
 * Adds a [Binding] for the provided parameters
 */
inline fun <reified T> Module.declare(
    name: String? = null,
    kind: Binding.Kind,
    scopeName: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = declare(T::class, name, kind, scopeName, override, eager, definition)

/**
 * Adds a [Binding] for the provided parameters
 */
fun <T> Module.declare(
    type: KClass<*>,
    name: String? = null,
    kind: Binding.Kind,
    scopeName: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    Binding.create(type, name, kind, scopeName, override, eager, definition)
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
    override: Boolean = false,
    eager: Boolean = false,
    definition: ModuleDefinition
) {
    module(com.ivianuu.injekt.module(name, scopeName, override, eager, definition))
}

/**
 * Adds a binding for [T] for a existing binding of [S]
 */
inline fun <reified T, reified S : T> Module.bind(
    bindingName: String? = null,
    existingName: String? = null
) {
    factory(bindingName) { get<S>(existingName) { it } }
}


operator fun Module.plus(module: Module): List<Module> = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>): List<Module> = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>): List<Module> = listOf(this) + modules