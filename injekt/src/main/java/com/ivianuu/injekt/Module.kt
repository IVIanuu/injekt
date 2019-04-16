/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

import java.util.*
import kotlin.reflect.KClass

/**
 * A module is the container for bindings
 */
class Module @PublishedApi internal constructor() {

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
        val isOverride = bindings.remove(binding.key) != null

        if (isOverride && !binding.override) {
            throw OverrideException("Try to override binding $binding")
        }

        bindings[binding.key] = binding

        return BindingContext(binding, this)
    }

}

/**
 * Creates a new [Module]
 */
inline fun module(definition: Module.() -> Unit = {}): Module = Module().apply(definition)

/**
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T> Module.single(
    name: Name? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding(
        type = T::class,
        name = name,
        kind = Binding.Kind.SINGLE,
        override = override,
        definition = definition
    )
)

/**
 * Adds all bindings of the [module]
 */
fun Module.module(module: Module) {
    module.bindings.forEach { add(it.value) }
}

/** Calls trough [Module.withBinding] */
inline fun <reified T> Module.withBinding(
    name: Name? = null,
    body: BindingContext<T>.() -> Unit
) {
    withBinding(T::class, name, body)
}

/**
 * Invokes the [body] in the [BindingContext] of the [Binding] with [type] and [name]
 */
inline fun <T> Module.withBinding(
    type: KClass<*>,
    name: Name? = null,
    body: BindingContext<T>.() -> Unit
) {
    // todo a little bit to hacky can we turn this into a clean thing?
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // the new factory acts as bridge and just calls trough the original implementation
    add(
        Binding(
            type = type,
            name = named(UUID.randomUUID().toString()),
            kind = Binding.Kind.FACTORY,
            definition = { component.get<T>(type, name) { it } }
        )
    ) withContext body
}

operator fun Module.plus(module: Module): List<Module> = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>): List<Module> = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>): List<Module> = listOf(this) + modules