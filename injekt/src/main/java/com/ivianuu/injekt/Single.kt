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
    noinline definition: Definition<T>
): BindingContext<T> = single(typeOf(), name, override, definition)

fun <T> ModuleBuilder.single(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = bind(
    definitionBinding(definition).asSingle(), type, name, override
)

inline fun <reified T> ModuleBuilder.singleWithState(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: StateDefinitionFactory.() -> StateDefinition<T>
): BindingContext<T> = singleWithState(typeOf(), name, override, definition)

fun <T> ModuleBuilder.singleWithState(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    definition: StateDefinitionFactory.() -> StateDefinition<T>
): BindingContext<T> = bind(stateDefinitionBinding(definition).asSingle(), type, name, override)

@Target(AnnotationTarget.CLASS)
annotation class Single

fun <T> Binding<T>.asSingle(): Binding<T> {
    return if (this is SingleBinding) this
    else SingleBinding(this)
}

private class SingleBinding<T>(private val binding: Binding<T>) : Binding<T>() {
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