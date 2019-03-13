package com.ivianuu.injekt

import java.util.*
import kotlin.reflect.KClass

/**
 * A module is the container for bindings
 */
class Module @PublishedApi internal constructor(
    val scope: Scope?,
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
        val scope = scope ?: binding.scope
        val override = override ?: binding.override
        val eager = eager ?: binding.eager

        if (binding.scope != scope
            || binding.eager != eager
            || binding.override != override
        ) {
            binding = binding.copy(
                scope = scope,
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
    scope: Scope? = null,
    override: Boolean? = null,
    eager: Boolean? = null,
    definition: ModuleDefinition = {}
): Module = Module(scope, eager, override).apply(definition)

/**
 * Adds all bindings of the [module]
 */
fun Module.module(module: Module) {
    module.bindings.forEach { add(it.value) }
}

/**
 * Adds all bindings of module
 */
inline fun Module.module(
    scope: Scope? = null,
    override: Boolean? = null,
    eager: Boolean? = null,
    definition: ModuleDefinition = {}
) {
    module(com.ivianuu.injekt.module(scope, override, eager, definition))
}

/** Calls trough [Module.withBinding] */
inline fun <reified T> Module.withBinding(
    qualifier: Qualifier? = null,
    body: BindingContext<T>.() -> Unit
) = withBinding(T::class, qualifier, body)

/**
 * Invokes the [body] in the [BindingContext] of the [Binding] with [type] and [qualifier]
 */
inline fun <T> Module.withBinding(
    type: KClass<*>,
    qualifier: Qualifier? = null,
    body: BindingContext<T>.() -> Unit
) {
    // todo a little bit to hacky can we turn this into a clean thing?
    // we create a additional binding because we have now reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // the new factory acts as bridge and just calls trough the original implementation
    add(
        Binding(
            type = type,
            qualifier = named(UUID.randomUUID().toString()),
            kind = FactoryKind,
            definition = { component.get<T>(type, qualifier) { it } }
        )
    ) withContext body
}

/**
 * Binds the [bindingType] to the existing [Binding] for [T] and [implementationName]
 */
inline fun <reified T> Module.bindType(
    bindingType: KClass<*>,
    implementationQualifier: Qualifier? = null
) {
    withBinding<T>(implementationQualifier) { bindType(bindingType) }
}

/**
 * Binds the [bindingQualifier] to the existing [Binding] for [T] and [implementationQualifier]
 */
inline fun <reified T> Module.bindQualifier(
    bindingQualifier: Qualifier,
    implementationQualifier: Qualifier? = null
) {
    withBinding<T>(implementationQualifier) { bindQualifier(bindingQualifier) }
}

operator fun Module.plus(module: Module): List<Module> = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>): List<Module> = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>): List<Module> = listOf(this) + modules