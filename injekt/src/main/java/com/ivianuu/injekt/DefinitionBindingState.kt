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

class StateDefinitionFactory {
    internal val links = mutableListOf<Link<*>>()

    inline fun <reified T> link(name: Any? = null): Link<T> =
        link(typeOf(), name)

    fun <T> link(type: Type<T>, name: Any? = null): Link<T> {
        val link = Link(type, name)
        links.add(link)
        return link
    }

    fun <T> definition(definition: StateDefinition<T>) = definition
}

class Link<T>(
    private val type: Type<T>,
    private val name: Any? = null
) : () -> T {

    private lateinit var binding: Binding<T>

    internal fun attach(component: Component) {
        binding = component.getBinding(type, name)
    }

    override fun invoke() = binding.get()
}

typealias StateDefinition<T> = (Parameters) -> T

fun <T> StatefulDefinitionBinding(
    block: StateDefinitionFactory.() -> StateDefinition<T>
): StatefulDefinitionBinding<T> {
    val factory = StateDefinitionFactory()
    val definition = factory.block()
    return StatefulDefinitionBinding(definition, factory.links)
}

class StatefulDefinitionBinding<T> internal constructor(
    private val definition: StateDefinition<T>,
    private val links: List<Link<*>>
) : Binding<T> {
    override fun attach(component: Component) {
        links.forEach { it.attach(component) }
    }

    override fun get(parameters: ParametersDefinition?): T =
        definition(parameters?.invoke() ?: emptyParameters())
}