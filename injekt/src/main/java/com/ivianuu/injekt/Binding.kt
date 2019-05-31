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
    val name: Qualifier?,
    val scope: Scope?,
    val override: Boolean,
    val definition: Definition<T>
) {

    val key = Key(type, name)

    val attributes = attributesOf()
    val additionalKeys = mutableListOf<Key>()

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
    name: Qualifier? = null,
    scope: Scope? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = binding(kind, T::class, name, scope, override, definition)

/**
 * Returns a new [Binding]
 */
fun <T> binding(
    kind: Kind,
    type: KClass<*>,
    name: Qualifier? = null,
    scope: Scope? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> = Binding(kind, type, name, scope, override, definition)

infix fun <T> Binding<T>.attributes(attributes: Attributes): Binding<T> {
    attributes.entries.forEach { this.attributes[it.key] = it.value }
    return this
}

infix fun <T> Binding<T>.attributes(attributes: Map<String, Any?>): Binding<T> {
    attributes.forEach { this.attributes[it.key] = it.value }
    return this
}

fun <T> Binding<T>.attributes(vararg attributes: Pair<String, Any?>): Binding<T> {
    attributes.forEach { this.attributes[it.first] = it.second }
    return this
}

fun <T> Binding<T>.attribute(key: String, value: Any?): Binding<T> {
    attributes[key] = value
    return this
}

infix fun <T> Binding<T>.attribute(pair: Pair<String, Any?>): Binding<T> {
    attributes[pair.first] = pair.second
    return this
}

fun <T> Binding<T>.additionalKeys(vararg keys: Key): Binding<T> {
    additionalKeys.addAll(keys)
    return this
}

infix fun <T> Binding<T>.additionalKeys(keys: Iterable<Key>): Binding<T> {
    additionalKeys.addAll(keys)
    return this
}

infix fun <T> Binding<T>.additionalKey(key: Key): Binding<T> {
    additionalKeys.add(key)
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
infix fun <T> Binding<T>.bindType(type: KClass<*>): Binding<T> =
    additionalKey(Key(type))

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

infix fun <T> Binding<T>.bindName(name: Qualifier): Binding<T> =
    additionalKey(Key(type, name))

fun <T> Binding<T>.bindNames(vararg names: Qualifier): Binding<T> {
    names.forEach { bindName(it) }
    return this
}

infix fun <T> Binding<T>.bindNames(names: Iterable<Qualifier>): Binding<T> {
    names.forEach { bindName(it) }
    return this
}

inline fun <reified T> Binding<*>.bindAlias(name: Qualifier) {
    bindAlias(T::class, name)
}

fun <T> Binding<T>.bindAlias(type: KClass<*>, name: Qualifier): Binding<T> =
    additionalKey(Key(type, name))

infix fun <T> Binding<T>.bindAlias(pair: Pair<KClass<*>, Qualifier>): Binding<T> {
    bindAlias(pair.first, pair.second)
    return this
}