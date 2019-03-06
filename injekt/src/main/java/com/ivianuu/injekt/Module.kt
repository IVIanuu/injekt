package com.ivianuu.injekt

import java.util.*
import kotlin.reflect.KClass

/**
 * A module is the container for bindings
 */
class Module @PublishedApi internal constructor(
    val scopeName: String?,
    val eager: Boolean?,
    val override: Boolean?
) {

    internal val bindings = linkedMapOf<Key, Binding<*>>()

    /**
     * Returns all [Binding]s of this module
     */
    fun getBindings(): Set<Binding<*>> = bindings.values.toSet()

    /**
     * Adds the [binding]
     */
    fun <T> add(
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

        val isOverride = bindings.remove(binding.key) != null

        if (isOverride && !binding.override) {
            throw OverrideException("Try to override binding $binding")
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
inline fun module(
    scopeName: String? = null,
    override: Boolean? = null,
    eager: Boolean? = null,
    definition: ModuleDefinition = {}
): Module = Module(scopeName, eager, override).apply(definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T> Module.factory(
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding.createFactory(
        type = T::class,
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
): BindingContext<T> = add(
    Binding.createSingle(
        type = T::class,
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
    module.bindings.forEach { add(it.value) }
}

/**
 * Adds all bindings of module
 */
inline fun Module.module(
    scopeName: String? = null,
    override: Boolean? = null,
    eager: Boolean? = null,
    definition: ModuleDefinition = {}
) {
    module(com.ivianuu.injekt.module(scopeName, override, eager, definition))
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
    add(
        Binding.createFactory(
            type = type,
            name = UUID.randomUUID().toString(),
            definition = { component.get<T>(type, name) { it } }
        )
    ) withContext body
}

/** Calls trough [Module.bindType] */
inline fun <reified T> Module.bindType(
    bindingType: KClass<*>,
    implementationName: String? = null
) {
    withBinding<T>(implementationName) { bindType(bindingType) }
}

/** Calls trough [Module.bindName] */
inline fun <reified T> Module.bindName(
    bindingName: String,
    implementationName: String? = null
) {
    withBinding<T>(implementationName) { bindName(bindingName) }
}

operator fun Module.plus(module: Module): List<Module> = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>): List<Module> = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>): List<Module> = listOf(this) + modules