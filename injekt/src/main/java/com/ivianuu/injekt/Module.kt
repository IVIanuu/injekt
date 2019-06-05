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

class SimpleModule(override val bindings: Map<Key, Binding<*>>) : Module

class ModuleBuilder {
    private val bindings = mutableMapOf<Key, Binding<*>>()

    fun bind(key: Key, binding: Binding<*>, override: Boolean = false) {
        if (bindings.put(key, binding) != null && !override) {
            error("Already declared binding for $key")
        }
    }

    fun include(module: Module) {
        module.bindings.forEach { bind(it.key, it.value) }
    }

    fun build(): Module = SimpleModule(bindings)
}

inline fun module(block: ModuleBuilder.() -> Unit): Module = ModuleBuilder()
    .apply(block).build()

// todo convenient extensions