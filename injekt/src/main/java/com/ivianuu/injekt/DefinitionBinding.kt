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

class StatefulDefinitionBuilder<T> {

    @PublishedApi internal lateinit var linker: Linker
    internal lateinit var definition: (Parameters) -> T

    inline fun <reified T> link(name: Qualifier? = null): Lazy<Binding<T>> =
        link(typeOf(), name)

    fun <T> link(type: Type<T>, name: Qualifier? = null): Lazy<Binding<T>> =
        lazy(LazyThreadSafetyMode.NONE) { linker.get(type, name) }

    fun definition(definition: (Parameters) -> T) {
        this.definition = definition
    }

}

class StatefulDefinitionBinding<T>(private val builder: StatefulDefinitionBuilder<T>) : Binding<T> {
    init {
        builder.definition
    }

    override fun link(linker: Linker) {
        builder.linker = linker
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