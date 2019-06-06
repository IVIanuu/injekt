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

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StatefulDefinitionBuilder<T> {

    private val links = mutableListOf<Link<*>>()

    internal lateinit var definition: (Parameters) -> T

    inline fun <reified T> link(name: Qualifier? = null): Link<T> =
        link(typeOf(), name)

    fun <T> link(type: Type<T>, name: Qualifier? = null): Link<T> {
        val link = Link(type, name)
        links.add(link)
        return link
    }

    fun definition(definition: (Parameters) -> T) {
        this.definition = definition
    }

    internal fun link(linker: Linker) {
        links.forEach { it.link(linker) }
    }

    class Link<T>(
        private val type: Type<T>,
        private val name: Qualifier? = null
    ) : ReadOnlyProperty<Nothing?, Binding<T>> {

        private lateinit var binding: Binding<T>

        fun link(linker: Linker) {
            binding = linker.get(type, name)
        }

        override fun getValue(thisRef: Nothing?, property: KProperty<*>) =
            binding
    }

}

class StatefulDefinitionBinding<T>(private val builder: StatefulDefinitionBuilder<T>) : Binding<T> {
    init {
        builder.definition
    }

    override fun link(linker: Linker) {
        builder.link(linker)
    }

    override fun get(parameters: ParametersDefinition?): T =
        builder.definition(parameters?.invoke() ?: emptyParameters())
}

class DefinitionBinding<T>(private val definition: Definition<T>) : Binding<T> {
    private lateinit var linker: Linker
    override fun link(linker: Linker) {
        this.linker = linker
    }

    override fun get(parameters: ParametersDefinition?): T {
        return try {
            definition.invoke(DefinitionContext(linker), parameters?.invoke() ?: emptyParameters())
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't instantiate", e) // todo
        }
    }

}

/**
 * Will called when ever a new instance is needed
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T