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
    val bindings: Map<Key, Binding<*>> get() = _bindings
    private val _bindings = mutableMapOf<Key, Binding<*>>()

    /**
     * The modules which are included in this one
     */
    val includes: Set<Module> get() = _includes
    private val _includes = mutableSetOf<Module>()

    /**
     * Adds the [binding]
     */
    fun bind(binding: Binding<*>) {
        if (_bindings.put(binding.key, binding) != null && !binding.override) {
            error("Already declared binding for ${binding.key}")
        }
    }

    /**
     * Adds the [module]
     */
    fun include(module: Module) {
        _includes.add(module)
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
    kind: Kind,
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = bind(kind, T::class, name, override, definition)

/**
 * Adds a [Binding]
 */
fun <T> Module.bind(
    kind: Kind,
    type: KClass<*>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    val binding = binding(kind, type, name, null, override, definition)
    bind(binding)
    return binding
}