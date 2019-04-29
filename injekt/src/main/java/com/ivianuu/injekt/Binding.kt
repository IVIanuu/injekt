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
    val kind: Kind,
    val type: KClass<*>,
    val name: Any? = null,
    val definition: Definition<T>
) {

    val key = Key(type, name)

    val attributes = attributesOf()
    val additionalBindings = mutableListOf<Binding<*>>()

    override fun toString(): String {
        return "$kind(" +
                "type=${type.java.name}, " +
                "name=$name)"
    }

}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T

/**
 * Returns a new [Binding]
 */
inline fun <reified T> binding(
    kind: Kind,
    name: Any? = null,
    noinline definition: Definition<T>
): Binding<T> = binding(kind, T::class, name, definition)

/**
 * Returns a new [Binding]
 */
fun <T> binding(
    kind: Kind,
    type: KClass<*>,
    name: Any? = null,
    definition: Definition<T>
): Binding<T> = Binding(kind, type, name, definition)

fun <T> Binding<T>.attributes(attributes: Attributes): Binding<T> {
    attributes(attributes.entries)
    return this
}

fun <T> Binding<T>.attributes(attributes: Map<String, Any?>): Binding<T> {
    attributes.forEach {
        this.attributes[it.key] = it.value
    }

    return this
}

fun <T> Binding<T>.attribute(key: String, value: Any?): Binding<T> {
    attributes[key] = value
    return this
}

fun <T> Binding<T>.additionalBindings(vararg bindings: Binding<*>): Binding<T> {
    additionalBindings.addAll(bindings)
    return this
}

fun <T> Binding<T>.additionalBindings(bindings: Iterable<Binding<*>>): Binding<T> {
    additionalBindings.addAll(bindings)
    return this
}

fun <T> Binding<T>.additionalBinding(binding: Binding<*>): Binding<T> {
    additionalBindings.add(binding)
    return this
}

/**
 * Adds a additional binding for [T]
 */
inline fun <reified T> Binding<*>.bindType() {
    bindType(T::class)
}

/**
 * Adds a additional binding for [type]
 */
infix fun <T> Binding<T>.bindType(type: KClass<*>): Binding<T> {
    additionalBinding(binding(kind, type, null, definition))
    return this
}

/**
 * Binds all of [types]
 */
fun <T> Binding<T>.bindTypes(vararg types: KClass<*>): Binding<T> {
    types.forEach { bindType(it) }
    return this
}

/**
 * Binds all of [types]
 */
infix fun <T> Binding<T>.bindTypes(types: Iterable<KClass<*>>): Binding<T> {
    types.forEach { bindTypes(it) }
    return this
}

infix fun <T> Binding<T>.bindName(name: Any): Binding<T> {
    additionalBinding(binding(kind, type, name, definition))
    return this
}

fun <T> Binding<T>.bindNames(vararg names: Any): Binding<T> {
    names.forEach { bindName(it) }
    return this
}

fun <T> Binding<T>.bindNames(names: Iterable<Any>): Binding<T> {
    names.forEach { bindName(it) }
    return this
}

inline fun <reified T> Binding<*>.bindAlias(name: Any) {
    bindAlias(T::class, name)
}

fun <T> Binding<T>.bindAlias(type: KClass<*>, name: Any): Binding<T> {
    additionalBinding(binding(kind, type, name, definition))
    return this
}

inline infix fun <T> Binding<T>.apply(block: Binding<T>.() -> Unit): Binding<T> {
    block()
    return this
}