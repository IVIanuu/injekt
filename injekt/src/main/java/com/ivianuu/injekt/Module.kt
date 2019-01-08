package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * A module is the container for definitions
 */
class Module internal constructor(
    val name: String?,
    val scopeId: String?,
    val eager: Boolean,
    val override: Boolean
) {

    internal val definitions = hashMapOf<Key, BeanDefinition<*>>()

    /**
     * Returns all [BeanDefinition]s of this module
     */
    fun getDefinitions(): Set<BeanDefinition<*>> = definitions.values.toSet()

    /**
     * Adds the [definition]
     */
    fun <T : Any> declare(
        definition: BeanDefinition<T>
    ): BindingContext<T> {
        var definition = definition
        val scopeId = scopeId ?: definition.scopeId
        val override = if (override) override else definition.override
        val eager = if (eager) eager else definition.eager

        if (definition.scopeId != scopeId
            || definition.eager != eager
            || definition.override != override
        ) {
            definition = definition.copy(
                scopeId = scopeId,
                eager = eager,
                override = override
            )
        }

        if (definitions.containsKey(definition.key) && !definition.override) {
            throw OverrideException("Try to override definition $definition but was already declared in $name")
        }

        definitions[definition.key] = definition

        return BindingContext(definition, this)
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
    scopeId: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: ModuleDefinition
): Module = Module(name, scopeId, eager, override).apply(definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T : Any> Module.factory(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = factory(T::class, name, scopeId, override, definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
fun <T : Any> Module.factory(
    type: KClass<T>,
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    type = type,
    name = name,
    kind = BeanDefinition.Kind.FACTORY,
    scopeId = scopeId,
    eager = false,
    override = override,
    definition = definition
)

/**
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T : Any> Module.single(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = single(T::class, name, scopeId, override, eager, definition)

/**
 * Provides scoped dependency which will be created once for each component
 */
fun <T : Any> Module.single(
    type: KClass<T>,
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    type = type,
    name = name,
    kind = BeanDefinition.Kind.SINGLE,
    scopeId = scopeId,
    override = override,
    eager = eager,
    definition = definition
)

/**
 * Adds a [BeanDefinition] for the provided parameters
 */
inline fun <reified T : Any> Module.declare(
    name: String? = null,
    kind: BeanDefinition.Kind,
    scopeId: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = declare(T::class, name, kind, scopeId, override, eager, definition)

/**
 * Adds a [BeanDefinition] for the provided parameters
 */
fun <T : Any> Module.declare(
    type: KClass<T>,
    name: String? = null,
    kind: BeanDefinition.Kind,
    scopeId: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    BeanDefinition.create(type, name, kind, scopeId, override, eager, definition)
)

/**
 * Adds all definitions of [module]
 */
fun Module.module(module: Module) {
    module.definitions.forEach { declare(it.value) }
}

/**
 * Adds all definitions of module
 */
fun Module.module(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: ModuleDefinition
) {
    module(com.ivianuu.injekt.module(name, scopeId, override, eager, definition))
}

/**
 * Adds a binding for [T] for a existing definition of [S]
 */
inline fun <reified T : Any, reified S : T> Module.bind(
    bindingName: String? = null,
    existingName: String? = null
): BindingContext<T> = factory(bindingName) { get<S>(existingName) }


operator fun Module.plus(module: Module): List<Module> = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>): List<Module> = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>): List<Module> = listOf(this) + modules