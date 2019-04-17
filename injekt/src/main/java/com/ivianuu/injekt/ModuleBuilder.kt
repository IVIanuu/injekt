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
class ModuleBuilder internal constructor() {

    private val bindings = arrayListOf<Binding<*>>()

    /**
     * Adds the [binding]
     */
    fun addBinding(binding: Binding<*>) {
        bindings.add(binding)
        binding.additionalBindings.forEach { addBinding(it) }
    }

    /**
     * Returns a new [Module] for this builder
     */
    fun build(): Module = Module(bindings)

}

/**
 * Adds a [Binding]
 */
inline fun <reified T> ModuleBuilder.bind(
    name: Any? = null,
    kind: Kind? = null,
    noinline definition: Definition<T>? = null,
    noinline block: (BindingBuilder<T>.() -> Unit)? = null
) {
    bind(T::class, name, kind, definition, block)
}

/**
 * Adds a [Binding]
 */
fun <T> ModuleBuilder.bind(
    type: KClass<*>,
    name: Any? = null,
    kind: Kind? = null,
    definition: Definition<T>? = null,
    block: (BindingBuilder<T>.() -> Unit)? = null
) {
    addBinding(binding(type, name, kind, definition, block))
}

/**
 * Adds all bindings of the [module]
 */
fun ModuleBuilder.module(module: Module) {
    module.bindings.forEach { addBinding(it) }
}

inline fun <reified T> ModuleBuilder.bridge(
    name: Any? = null,
    noinline block: BindingBuilder<T>.() -> Unit
) {
    bridge(T::class, name, block)
}

fun <T> ModuleBuilder.bridge(
    type: KClass<*>,
    name: Any? = null,
    block: BindingBuilder<T>.() -> Unit
) {
    // todo this is a little hacky can we turn this into a clean thing?
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // the new factory acts as bridge and just calls trough the original implementation
    factoryBuilder<T>(type, UUID.randomUUID().toString()) {
        definition { get(type, name) { it } }
        block()
    }
}