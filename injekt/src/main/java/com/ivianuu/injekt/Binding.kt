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

import kotlin.reflect.KClass

/**
 * Represents a dependency binding.
 */
class Binding<T> internal constructor(
    internal val key: Key,
    val kind: Kind,
    val definition: Definition<T>,
    val attributes: Attributes,
    val additionalBindings: List<Binding<*>>
) {

    val type = key.type
    val name = key.name

    override fun toString(): String {
        return "$kind(" +
                "type=${type.java.name}, " +
                "name=$name, " +
                ")"
    }

}

/**
 * Returns a new [Binding] configured by [block]
 */
fun <T> binding(
    type: KClass<*>? = null,
    name: Any? = null,
    kind: Kind? = null,
    definition: Definition<T>? = null,
    block: (BindingBuilder<T>.() -> Unit)? = null
): Binding<T> {
    return BindingBuilder<T>()
        .apply {
            type?.let { this.type = it }
            name?.let { this.name = it }
            kind?.let { this.kind = it }
            definition?.let { this.definition = it }
            block?.invoke(this)
        }
        .build()
}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T