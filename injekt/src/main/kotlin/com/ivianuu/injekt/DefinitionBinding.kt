/*
 * Copyright 2019 Manuel Wrage
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

typealias Definition<T> = DefinitionContext.(Parameters?) -> T

interface DefinitionContext {
    fun <T> get(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T = get(keyOf(type, name), parameters)

    fun <T> get(key: Key, parameters: ParametersDefinition? = null): T

    operator fun <T> Parameters?.component1(): T = this!![0]
    operator fun <T> Parameters?.component2(): T = this!![1]
    operator fun <T> Parameters?.component3(): T = this!![2]
    operator fun <T> Parameters?.component4(): T = this!![3]
    operator fun <T> Parameters?.component5(): T = this!![4]
}

fun <T> definitionBinding(
    optimizing: Boolean = true,
    definition: Definition<T>
): Binding<T> {
    return if (optimizing) UnlinkedOptimizingDefinitionBinding(definition)
    else UnlinkedDefinitionBinding(definition)
}

inline fun <reified T> DefinitionContext.get(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(typeOf(), name, parameters)

private class UnlinkedDefinitionBinding<T>(
    private val definition: Definition<T>
) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> = LinkedDefinitionBinding(
        linker.component, definition
    )
}

private class LinkedDefinitionBinding<T>(
    private val component: Component,
    private val definition: Definition<T>
) : LinkedBinding<T>(), DefinitionContext {

    override fun <T> get(key: Key, parameters: ParametersDefinition?): T =
        component.get(key, parameters)

    override fun invoke(parameters: ParametersDefinition?): T =
        definition(this, parameters?.invoke())

}

private class UnlinkedOptimizingDefinitionBinding<T>(
    private val definition: Definition<T>
) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        LinkedOptimizingDefinitionBinding(linker.component, definition)
}

private class LinkedOptimizingDefinitionBinding<T>(
    private val component: Component,
    private val definition: Definition<T>
) : LinkedBinding<T>(), DefinitionContext {

    private var bindings = arrayOfNulls<LinkedBinding<*>>(5)
    private var currentIndex = 0

    override fun <T> get(key: Key, parameters: ParametersDefinition?): T {
        return if (currentIndex > bindings.lastIndex) {
            bindings = bindings.copyOf(currentIndex + 5)
            val binding = component.linker.get<T>(key)
            bindings[currentIndex] = binding
            ++currentIndex
            binding
        } else {
            var binding = bindings[currentIndex]
            if (binding == null) {
                binding = component.linker.get<T>(key)
                bindings[currentIndex] = binding
            }

            ++currentIndex

            binding
        }(parameters) as T
    }

    override fun invoke(parameters: ParametersDefinition?): T {
        currentIndex = 0
        return definition(this, parameters?.invoke())
    }

}