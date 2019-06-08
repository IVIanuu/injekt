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

typealias Definition<T> = DefinitionContext.(Parameters) -> T

fun <T> definitionBinding(definition: Definition<T>): Binding<T> =
    DefinitionBinding(definition)

interface DefinitionContext {
    fun <T> get(type: Type<T>, name: Any? = null): T =
        get(keyOf(type, name))

    fun <T> get(key: Key): T
}

inline fun <reified T> DefinitionContext.get(name: Any? = null): T = get(typeOf<T>(), name)

private class DefinitionBinding<T>(private val definition: Definition<T>) : LinkedBinding<T>(),
    DefinitionContext {

    private val bindings = arrayListOf<LinkedBinding<*>>()
    @PublishedApi internal var currentIndex = -1
    private lateinit var component: Component

    override fun <T> get(key: Key): T {
        ++currentIndex
        var binding = bindings.getOrNull(currentIndex)
        if (binding == null) {
            binding = component.linker.get<T>(key)
            bindings.add(currentIndex, binding)
        }

        return binding.get() as T
    }

    override fun get(parameters: ParametersDefinition?): T {
        currentIndex = -1
        return definition(this, parameters?.invoke() ?: emptyParameters())
    }

    override fun attached(component: Component) {
        this.component = component
    }
}