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

import kotlin.properties.Delegates
import kotlin.reflect.KClass

/**
 * Builder for [Binding]s
 */
class BindingBuilder<T> @PublishedApi internal constructor() {

    var type: KClass<*> by Delegates.notNull()
        private set
    var name: Name? = null
        private set
    var kind: Binding.Kind by Delegates.notNull()
        private set
    var definition: Definition<T> by Delegates.notNull()
        private set
    var attributes = Attributes()
        private set
    var override = false
        private set

    val additionalBindings: List<Binding<*>> get() = _additionalBindings
    private val _additionalBindings = arrayListOf<Binding<*>>()

    fun type(type: KClass<*>) {
        this.type = type
    }

    fun name(name: Name?) {
        this.name = name
    }

    fun kind(kind: Binding.Kind) {
        this.kind = kind
    }

    fun definition(definition: Definition<T>) {
        this.definition = definition
    }

    fun attributes(attributes: Attributes) {
        this.attributes = attributes
    }

    fun override(override: Boolean = true) {
        this.override = override
    }

    fun additionalBinding(binding: Binding<*>) {
        _additionalBindings.add(binding)
    }

    /**
     * Builds the [Binding] from this builder
     */
    fun build(): Binding<T> = Binding(
        Key(type, name),
        kind, definition, attributes, override, additionalBindings
    )

}

/**
 * Returns a new builder for this binding
 */
fun <T> Binding<T>.toBuilder(): BindingBuilder<T> = BindingBuilder<T>().apply {
    type(this@toBuilder.type)
    name(this@toBuilder.name)
    kind(this@toBuilder.kind)
    definition(this@toBuilder.definition)
    attributes(this@toBuilder.attributes)
    override(this@toBuilder.override)
}

/**
 * Creates a copy of this builder
 */
fun <T> BindingBuilder<T>.copy(): BindingBuilder<T> {
    val other = BindingBuilder<T>()

    other.type(type)
    other.name(name)
    other.kind(kind)
    other.definition(definition)
    other.attributes(attributes)
    other.override(override)

    return other
}

/**
 * Applies the factory kind
 */
fun BindingBuilder<*>.factory() {
    kind(Binding.Kind.FACTORY)
}

/**
 * Applies the single kind
 */
fun BindingBuilder<*>.single() {
    kind(Binding.Kind.SINGLE)
}

/**
 * Adds a additional binding for [T]
 */
inline fun <reified T> BindingBuilder<*>.bindType() {
    bindType(T::class)
}

/**
 * Adds a additional binding for [type]
 */
fun BindingBuilder<*>.bindType(type: KClass<*>) {
    val copy = copy()
    copy.type(type)
    copy.name(null)
    additionalBinding(copy.build())
}

/**
 * Binds all of [types]
 */
fun BindingBuilder<*>.bindTypes(vararg types: KClass<*>) {
    types.forEach { bindType(it) }
}

/**
 * Binds all of [types]
 */
fun BindingBuilder<*>.bindTypes(types: Iterable<KClass<*>>) {
    types.forEach { bindTypes(it) }
}

/**
 * Binds all of [types]
 */
fun BindingBuilder<*>.bindTypes(type: KClass<*>) {
    bindType(type)
}


fun BindingBuilder<*>.bindName(name: Name) {
    val copy = copy()
    copy.name(name)
    additionalBinding(copy.build())
}

fun BindingBuilder<*>.bindNames(vararg names: Name) {
    names.forEach { bindName(it) }
}

fun BindingBuilder<*>.bindNames(names: Iterable<Name>) {
    names.forEach { bindName(it) }
}

fun BindingBuilder<*>.bindNames(name: Name) {
    bindName(name)
}

inline fun <reified T> BindingBuilder<*>.bindAlias(name: Name) {
    bindAlias(T::class, name)
}

fun BindingBuilder<*>.bindAlias(type: KClass<*>, name: Name) {
    val copy = copy()
    copy.type(type)
    copy.name(name)
    additionalBinding(copy.build())
}