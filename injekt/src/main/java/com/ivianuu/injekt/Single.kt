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

// todo return types

inline fun <reified T> ModuleBuilder.single(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) = single(typeOf(), name, override, definition)

fun <T> ModuleBuilder.single(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
) = bind(keyOf(type, name), SingleBinding(DefinitionBinding(definition)), override)

@Target(AnnotationTarget.CLASS)
annotation class Single

class SingleBinding<T>(private val binding: Binding<T>) : Binding<T> {
    private var _value: Any? = UNINITIALIZED

    override fun link(context: DefinitionContext) {
        binding.link(context)
    }

    override fun get(parameters: ParametersDefinition?): T {
        var value = _value
        if (value !== UNINITIALIZED) {
            // todo InjektPlugins.logger?.info("${context.component.scopeName()} Return existing instance $binding")
            return value as T
        }

        synchronized(this) {
            value = _value
            if (value !== UNINITIALIZED) {
                // todo InjektPlugins.logger?.info("${context.component.scopeName()} Return existing instance $binding")
                return@get value as T
            }

            // todo InjektPlugins.logger?.info("${context.component.scopeName()} Create instance $binding")
            value = binding.get(parameters)
            _value = value
            return@get value as T
        }
    }

    private companion object UNINITIALIZED

}