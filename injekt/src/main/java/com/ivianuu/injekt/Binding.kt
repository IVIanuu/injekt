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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Binding<*>) return false

        if (key != other.key) return false
        if (kind != other.kind) return false
        if (attributes != other.attributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }

    override fun toString(): String {
        return "$kind(" +
                "type=${type.java.name}, " +
                "name=$name" +
                "attributes=$attributes" +
                ")"
    }

}

/**
 * Returns a new [Binding] configured by [block]
 */
fun <T> binding(block: BindingBuilder<T>.() -> Unit): Binding<T> {
    return BindingBuilder<T>()
        .apply(block)
        .build()
}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T