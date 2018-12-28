package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Returns a [Component] which contains [modules]
 */
fun component(vararg modules: Module) =
    component(modules = modules.toList())

/**
 * Returns a [Component] which contains [modules]
 * And depends on any of [dependsOn]
 */
fun component(
    modules: Collection<Module> = emptyList(),
    dependencies: Collection<Component> = emptyList(),
    name: String? = null
) = Component(name).apply {
    dependencies.forEach { addDependency(it) }
    modules.forEach { addModule(it) }
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