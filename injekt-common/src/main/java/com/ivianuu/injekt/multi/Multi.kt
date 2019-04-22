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

package com.ivianuu.injekt.multi

import com.ivianuu.injekt.*
import kotlin.reflect.KClass

/**
 * Kind for multi instances
 */
object MultiKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = MultiInstance(binding)
    override fun toString(): String = "Multi"
}

/**
 * Applies the [MultiKind]
 */
fun BindingBuilder<*>.multi() {
    kind = MultiKind
}

/**
 * Adds a [Binding] which will be created once per [Component]
 */
inline fun <reified T> ModuleBuilder.multi(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) {
    multi(T::class, name, override, definition)
}

/**
 * Adds a [Binding] which will be created once per [Component]
 */
fun <T> ModuleBuilder.multi(
    type: KClass<*>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>
) {
    bind(type, name, MultiKind, override, definition)
}

/**
 * Adds a [Binding] which will be created once per [Component]
 */
inline fun <reified T> ModuleBuilder.multiBuilder(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>? = null,
    noinline block: BindingBuilder<T>.() -> Unit
) {
    multiBuilder(T::class, name, override, definition, block)
}

/**
 * Adds a [Binding] which will be created once per [Component]
 */
fun <T> ModuleBuilder.multiBuilder(
    type: KClass<*>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>? = null,
    block: BindingBuilder<T>.() -> Unit
) {
    bind(type, name, MultiKind, override, definition, block)
}

private class MultiInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private val values = linkedMapOf<Int, T>()

    override fun get(parameters: ParametersDefinition?): T {
        checkNotNull(parameters) { "Parameters cannot be null" }

        val params = parameters()

        val key = params.values.hashCode()

        var value = values[key]

        return if (value == null && !values.containsKey(key)) {
            InjektPlugins.logger?.info("Create multi instance for params $params $binding")
            value = create(parameters)
            values[key] = value
            value
        } else {
            InjektPlugins.logger?.info("Return existing multi instance for params $params $binding")
            value as T
        }
    }

}