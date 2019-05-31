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
 * Adds a [Binding] which will be created once per [Component]
 */
inline fun <reified T> Module.multi(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = multi(T::class, name, override, definition)

/**
 * Adds a [Binding] which will be created once per [Component]
 */
fun <T> Module.multi(
    type: KClass<*>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> = bind(MultiKind, type, name, override, definition)

private class MultiInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private val values = mutableMapOf<Int, T>()

    override fun get(requestingContext: DefinitionContext, parameters: ParametersDefinition?): T {
        requireNotNull(parameters) { "Parameters cannot be null" }

        val params = parameters()

        val key = params.values.hashCode()

        var value = values[key]

        return if (value == null && !values.containsKey(key)) {
            InjektPlugins.logger?.info("Create multi instance for params $params $binding")
            value = create(attachedContext, parameters)
            values[key] = value
            value
        } else {
            InjektPlugins.logger?.info("Return existing multi instance for params $params $binding")
            value as T
        }
    }

}