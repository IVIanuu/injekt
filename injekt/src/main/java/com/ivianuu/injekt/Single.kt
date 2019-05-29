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
 * Kind for single instances
 */
object SingleKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = SingleInstance(binding)
    override fun toString(): String = "Single"
}

/**
 * Adds a [Binding] which will be created once per [Component]
 */
inline fun <reified T> Module.single(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = bind(SingleKind, T::class, name, override, definition)

private object UNINITIALIZED

private class SingleInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private var _value: Any? = UNINITIALIZED

    override fun get(requestingContext: DefinitionContext, parameters: ParametersDefinition?): T {
        var value = _value
        if (value !== UNINITIALIZED) {
            InjektPlugins.logger?.info("Return existing instance $binding")
            return value as T
        }

        synchronized(this) {
            value = _value
            if (value !== UNINITIALIZED) {
                InjektPlugins.logger?.info("Return existing instance $binding")
                return@get value as T
            }

            InjektPlugins.logger?.info("Create instance $binding")
            value = create(attachedContext, parameters)
            _value = value
            return@get value as T
        }
    }

}