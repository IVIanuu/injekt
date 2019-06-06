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

inline fun <reified T> ModuleBuilder.single(
    name: Any? = null,
    override: Boolean = false,
    noinline block: StateDefinitionFactory.() -> Definition<T>
): BindingContext<T> = single(typeOf(), name, override, block)

fun <T> ModuleBuilder.single(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    block: StateDefinitionFactory.() -> Definition<T>
): BindingContext<T> = bind(
    DefinitionBinding(block).asSingle(), type, name, override
)

@Target(AnnotationTarget.CLASS)
annotation class Single

fun <T> Binding<T>.asSingle(): Binding<T> {
    return if (this is SingleBinding) this
    else SingleBinding(this)
}

internal class SingleBinding<T>(private val binding: Binding<T>) : Binding<T> {
    private var _value: Any? = UNINITIALIZED

    override fun attach(component: Component) {
        binding.attach(component)
    }

    override fun get(parameters: ParametersDefinition?): T {
        var value = _value
        if (value !== UNINITIALIZED) {
            return value as T
        }

        synchronized(this) {
            value = _value
            if (value !== UNINITIALIZED) {
                return@get value as T
            }

            value = binding(parameters)
            _value = value
            return@get value as T
        }
    }

    private companion object UNINITIALIZED

}