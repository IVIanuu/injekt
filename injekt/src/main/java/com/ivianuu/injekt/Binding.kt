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
    val type: KClass<*>,
    val qualifier: Qualifier?,
    val kind: Kind,
    val definition: Definition<T>,
    val attributes: Attributes,
    val override: Boolean,
    val eager: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Binding<*>) return false

        if (key != other.key) return false
        if (kind != other.kind) return false
        if (attributes != other.attributes) return false
        if (override != other.override) return false
        if (eager != other.eager) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + override.hashCode()
        result = 31 * result + eager.hashCode()
        return result
    }

    override fun toString(): String {
        return "${kind.asString()}(" +
                "type=${type.java.name}, " +
                "qualifier=$qualifier" +
                ")"
    }
}

inline fun <reified T> Binding(
    qualifier: Qualifier? = null,
    kind: Kind,
    attributes: Attributes = attributesOf(),
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> {
    return Binding(
        T::class,
        qualifier,
        kind,
        attributes,
        override,
        eager,
        definition
    )
}

fun <T> Binding(
    type: KClass<*>,
    qualifier: Qualifier? = null,
    kind: Kind,
    attributes: Attributes = attributesOf(),
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    return Binding(
        Key(type, qualifier), type, qualifier, kind,
        definition, attributes, override, eager
    )
}

fun <T> Binding<T>.copy(
    type: KClass<*> = this.type,
    qualifier: Qualifier? = this.qualifier,
    kind: Kind = this.kind,
    attributes: Attributes = this.attributes,
    override: Boolean = this.override,
    eager: Boolean = this.eager,
    definition: Definition<T> = this.definition
): Binding<T> {
    return Binding(
        Key(type, qualifier),
        type,
        qualifier,
        kind,
        definition,
        attributes,
        override,
        eager
    )
}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T