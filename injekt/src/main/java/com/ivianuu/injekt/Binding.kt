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
    val key: Key,
    val kind: Kind,
    val definition: Definition<T>,
    val attributes: Attributes,
    val override: Boolean
) {

    val type = key.type
    val name = key.name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Binding<*>) return false

        if (key != other.key) return false
        if (kind != other.kind) return false
        if (attributes != other.attributes) return false
        if (override != other.override) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + override.hashCode()
        return result
    }

    override fun toString(): String {
        return "${kind}(" +
                "type=${type.java.name}, " +
                "name=$name" +
                ")"
    }

    enum class Kind { FACTORY, SINGLE }
}

inline fun <reified T> Binding(
    name: Any? = null,
    kind: Binding.Kind,
    attributes: Attributes = Attributes(),
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> {
    return Binding(T::class, name, kind, attributes, override, definition)
}

fun <T> Binding(
    type: KClass<*>,
    name: Any? = null,
    kind: Binding.Kind,
    attributes: Attributes = Attributes(),
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    return Binding(Key(type, name), kind, definition, attributes, override)
}

fun <T> Binding<T>.copy(
    type: KClass<*> = this.type,
    name: Any? = this.name,
    kind: Binding.Kind = this.kind,
    attributes: Attributes = Attributes(),
    override: Boolean = this.override,
    definition: Definition<T> = this.definition
): Binding<T> {
    return Binding(
        Key(type, name),
        kind,
        definition,
        attributes,
        override
    )
}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T