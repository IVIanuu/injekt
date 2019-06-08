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

typealias Definition<T> = DefinitionContext.(Parameters?) -> T

interface DefinitionContext {
    fun <T> get(type: Type<T>, name: Any? = null): T =
        get(keyOf(type, name))

    fun <T> get(key: Key): T

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

inline fun <reified T> DefinitionContext.get(name: Any? = null): T = get(typeOf(), name)

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

    override fun <T> get(key: Key): T = component.get(key)

    override fun get(parameters: ParametersDefinition?): T =
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
    @PublishedApi internal var currentIndex = -1

    override fun <T> get(key: Key): T {
        ++currentIndex
        return if (currentIndex > bindings.lastIndex) {
            bindings = bindings.copyOf(currentIndex + 1)
            val binding = component.linker.get<T>(key)
            bindings[currentIndex] = binding
            binding
        } else {
            var binding = bindings[currentIndex]
            if (binding == null) {
                binding = component.linker.get<T>(key)
                bindings[currentIndex] = binding
            }

            binding
        }.get() as T
    }

    override fun get(parameters: ParametersDefinition?): T {
        currentIndex = -1
        return definition(this, parameters?.invoke())
    }

}