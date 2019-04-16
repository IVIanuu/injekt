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
 * Builder for [Module]s
 */
class ModuleBuilder @PublishedApi internal constructor() {

    private val bindings = linkedMapOf<Key, Binding<*>>()

    /**
     * Adds the [binding]
     */
    fun <T> bind(binding: Binding<T>) {
        val isOverride = bindings.remove(binding.key) != null

        if (isOverride && !binding.override) {
            throw OverrideException("Try to override binding $binding")
        }

        bindings[binding.key] = binding

        binding.additionalBindings.forEach { bind(it) }
    }

    /**
     * Returns a new [Module] for this builder
     */
    fun build(): Module = Module(bindings)

}

inline fun <reified T> ModuleBuilder.bind(
    name: Name? = null,
    kind: Binding.Kind? = null,
    override: Boolean = false,
    noinline bindingDefinition: Definition<T>? = null,
    noinline definition: BindingBuilder<T>.() -> Unit = {}
) {
    bind(T::class, name, kind, override, bindingDefinition, definition)
}

fun <T> ModuleBuilder.bind(
    type: KClass<*>,
    name: Name? = null,
    kind: Binding.Kind? = null,
    override: Boolean = false,
    bindingDefinition: Definition<T>? = null,
    definition: BindingBuilder<T>.() -> Unit = {}
) {
    bind(
        binding<T> {
            type(type)
            name?.let { name(it) }
            kind?.let { kind(it) }
            override(override)
            bindingDefinition?.let { definition(it) }
            definition()
        }
    )
}

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T> ModuleBuilder.factory(
    name: Name? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) {
    bind(name, Binding.Kind.FACTORY, override, bindingDefinition = definition)
}

inline fun <reified T> ModuleBuilder.factoryBuilder(
    name: Name? = null,
    override: Boolean = false,
    noinline bindingDefinition: Definition<T>? = null,
    noinline definition: BindingBuilder<T>.() -> Unit
) {
    bind(name, Binding.Kind.FACTORY, override, bindingDefinition, definition)
}

/**
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T> ModuleBuilder.single(
    name: Name? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) {
    bind(name, Binding.Kind.SINGLE, override, bindingDefinition = definition)
}

inline fun <reified T> ModuleBuilder.singleBuilder(
    name: Name? = null,
    override: Boolean = false,
    noinline bindingDefinition: Definition<T>? = null,
    noinline definition: BindingBuilder<T>.() -> Unit
) {
    bind(name, Binding.Kind.SINGLE, override, bindingDefinition, definition)
}

/**
 * Adds all bindings of the [module]
 */
fun ModuleBuilder.module(module: Module) {
    module.bindings.forEach { bind(it.value) }
}

/** Calls trough [ModuleBuilder.bridge] */
inline fun <reified T> ModuleBuilder.bridge(
    name: Name? = null,
    noinline body: BindingBuilder<T>.() -> Unit
) {
    bridge(T::class, name, body)
}

/**
 * Allows to add additional bindings to an existing binding
 */
fun <T> ModuleBuilder.bridge(
    type: KClass<*>,
    name: Name? = null,
    body: BindingBuilder<T>.() -> Unit
) {
    // todo this is a little hacky can we turn this into a clean thing?
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // the new factory acts as bridge and just calls trough the original implementation
    bind(
        binding<T> {
            type(type)
            name(named(UUID.randomUUID().toString()))
            kind(Binding.Kind.FACTORY)
            definition { component.get(type, name) }
            body()
        }
    )
}