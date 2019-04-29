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

import kotlin.reflect.KClass

/**
 * A module is a collection of [Binding]
 */
class Module internal constructor() {

    /**
     * All bindings of this module
     */
    val bindings: Collection<Binding<*>> get() = _bindings.values
    private val _bindings = mutableMapOf<Key, Binding<*>>()

    /**
     * Adds the [binding]
     */
    fun addBinding(binding: Binding<*>) {
        require(_bindings.remove(binding.key) == null || binding.override) {
            "Already declared binding with key ${binding.key}"
        }

        _bindings[binding.key] = binding
        binding.additionalBindings.forEach { addBinding(it) }
    }

}

/**
 * Returns a new [Module] configured by [block]
 */
fun module(block: (Module.() -> Unit)? = null): Module {
    return Module()
        .apply { block?.invoke(this) }
}

/**
 * Adds a [Binding]
 */
inline fun <reified T> Module.bind(
    name: Any? = null,
    kind: Kind? = null,
    override: Boolean = false,
    noinline definition: Definition<T>? = null,
    noinline block: (BindingBuilder<T>.() -> Unit)? = null
) {
    bind(T::class, name, kind, override, definition, block)
}

/**
 * Adds a [Binding]
 */
fun <T> Module.bind(
    type: KClass<*>,
    name: Any? = null,
    kind: Kind? = null,
    override: Boolean = false,
    definition: Definition<T>? = null,
    block: (BindingBuilder<T>.() -> Unit)? = null
) {
    addBinding(binding(type, name, kind, override, definition, block))
}

/**
 * Adds all bindings of the [module] to this module
 */
fun Module.module(module: Module) {
    module.bindings.forEach { addBinding(it) }
}