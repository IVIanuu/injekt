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

package com.ivianuu.injekt.eager

import com.ivianuu.injekt.*
import kotlin.reflect.KClass

/**
 * Eager kind
 */
object EagerKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> =
        EagerInstance(binding)

    override fun toString(): String = "Eager"
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
inline fun <reified T> ModuleBuilder.eager(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) {
    eager(T::class, name, override, definition)
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
fun <T> ModuleBuilder.eager(
    type: KClass<*>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>
) {
    bind(type, name, EagerKind, override, definition)
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
inline fun <reified T> ModuleBuilder.eagerBuilder(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>? = null,
    noinline block: BindingBuilder<T>.() -> Unit
) {
    eagerBuilder(T::class, name, override, definition, block)
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
fun <T> ModuleBuilder.eagerBuilder(
    type: KClass<*>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>? = null,
    block: BindingBuilder<T>.() -> Unit
) {
    bind(type, name, EagerKind, override, definition, block)
}

private object UNINITIALIZED

private class EagerInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private var _value: Any? = UNINITIALIZED

    override fun get(parameters: ParametersDefinition?): T {
        var value = _value
        if (value !== UNINITIALIZED) {
            InjektPlugins.logger?.info("Return existing eager instance $binding")
            return value as T
        }

        synchronized(this) {
            value = _value
            if (value !== UNINITIALIZED) {
                InjektPlugins.logger?.info("Return existing eager instance $binding")
                return@get value as T
            }

            InjektPlugins.logger?.info("Initialize eager instance $binding")
            value = create(parameters)
            _value = value
            return@get value as T
        }
    }

    override fun setDefinitionContext(context: DefinitionContext) {
        super.setDefinitionContext(context)
        get(null)
    }

}