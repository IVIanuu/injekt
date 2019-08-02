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

class StateDefinitionContext {
    internal val links = mutableListOf<Link<*>>()

    inline fun <reified T> link(name: Any? = null): Link<T> =
        link(typeOf(), name)

    fun <T> link(type: Type<T>, name: Any? = null): Link<T> {
        val link = Link(type, name)
        links += link
        return link
    }

    fun <T> definition(definition: StateDefinition<T>): StateDefinition<T> = definition
}

class Link<T> internal constructor(
    private val type: Type<T>,
    private val name: Any? = null
) {

    private lateinit var binding: LinkedBinding<T>

    internal fun link(linker: Linker) {
        binding = linker.get(type, name)
    }

    operator fun invoke(parameters: ParametersDefinition? = null) =
        binding(parameters)
}

typealias StateDefinition<T> = (Parameters?) -> T

fun <T> stateDefinitionBinding(block: StateDefinitionContext.() -> StateDefinition<T>): Binding<T> {
    val context = StateDefinitionContext()
    val definition = context.block()
    return UnlinkedStateDefinitionBinding(definition, context.links)
}

private class UnlinkedStateDefinitionBinding<T>(
    private val definition: StateDefinition<T>,
    private val links: List<Link<*>>
) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> {
        links.forEach { it.link(linker) }
        return LinkedStateDefinitionBinding(definition)
    }
}

private class LinkedStateDefinitionBinding<T>(
    private val definition: StateDefinition<T>
) : LinkedBinding<T>() {
    override fun invoke(parameters: ParametersDefinition?): T =
        definition(parameters?.invoke())
}