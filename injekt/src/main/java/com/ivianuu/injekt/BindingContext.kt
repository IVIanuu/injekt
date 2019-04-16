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
 * Binding context
 */
data class BindingContext<T>(val binding: Binding<T>, val moduleBuilder: ModuleBuilder)

/**
 * Invokes the [body]
 */
inline infix fun <T> BindingContext<T>.withContext(body: BindingContext<T>.() -> Unit): BindingContext<T> {
    body()
    return this
}

/**
 * Adds this [Binding] to [type]
 */
infix fun <T> BindingContext<T>.bindType(type: KClass<*>): BindingContext<T> {
    val copy = binding.copy(type = type, name = null)
    moduleBuilder.addBinding(copy)
    return this
}

/**
 * Adds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Array<KClass<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

/**
 * Adds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Iterable<KClass<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

/**
 * Adds this [Binding] to [name]
 */
infix fun <T> BindingContext<T>.bindName(name: Name): BindingContext<T> {
    val copy = binding.copy(name = name)
    moduleBuilder.addBinding(copy)
    return this
}

/**
 * Adds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindNames(names: Array<out Name>): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}

/**
 * Adds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindNames(names: Iterable<Name>): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}