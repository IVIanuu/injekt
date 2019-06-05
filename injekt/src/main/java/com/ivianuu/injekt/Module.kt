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

/**
 * A module is a collection of [Binding]s to drive [Component]s
 */
interface Module {
    val bindings: Map<Key, Binding<*>>
}

private class SimpleModule(override val bindings: Map<Key, Binding<*>>) : Module

class ModuleBuilder {
    private val bindings = mutableMapOf<Key, Binding<*>>()

    fun <T> add(
        binding: Binding<T>,
        key: Key,
        override: Boolean = false
    ): BindingContext<T> {
        if (bindings.put(key, binding) != null && !override) {
            error("Already declared binding for $key")
        }

        return BindingContext(binding, key, override, this)
    }

    fun include(module: Module) {
        module.bindings.forEach { add(it.value, it.key) }
    }

    fun build(): Module = module(bindings)
}

fun module(bindings: Map<Key, Binding<*>>): Module = SimpleModule(bindings)

inline fun module(block: ModuleBuilder.() -> Unit): Module = ModuleBuilder()
    .apply(block).build()

inline fun <reified T> ModuleBuilder.add(
    binding: Binding<T>,
    name: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = add(binding, typeOf(), name, override)

fun <T> ModuleBuilder.add(
    binding: Binding<T>,
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = add(binding, keyOf(type, name), override)