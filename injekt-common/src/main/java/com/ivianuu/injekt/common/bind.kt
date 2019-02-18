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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.withBinding
import kotlin.reflect.KClass

/**
 * Binds this [Binding] to [type]
 */
infix fun <T> BindingContext<T>.bindType(type: KClass<*>): BindingContext<T> {
    val copy = binding.copy(
        key = Key(type, name = null),
        type = type, name = null
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Array<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bindType(it) }
}

/**
 * Binds this [Binding] to [types]
 */
infix fun <T> BindingContext<T>.bindTypes(types: Iterable<KClass<*>>): BindingContext<T> = apply {
    types.forEach { bindType(it) }
}

/**
 * Binds this [Binding] to [name]
 */
infix fun <T> BindingContext<T>.bindName(name: String): BindingContext<T> {
    val copy = binding.copy(
        key = Key(binding.key.type, name),
        name = name
    )
    module.declare(copy)
    return this
}

/**
 * Binds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindNames(names: Array<String>): BindingContext<T> = apply {
    names.forEach { bindName(it) }
}

/**
 * Binds this [Binding] to [names]
 */
infix fun <T> BindingContext<T>.bindNames(names: Iterable<String>): BindingContext<T> = apply {
    names.forEach { bindName(it) }
}

/** Calls trough [Module.bindType] */
inline fun <reified T, reified S> Module.bindType(
    implementationName: String? = null
) {
    bindType<T, S>(T::class, S::class, implementationName)
}

/**
 * Adds a binding for [bindingType] to a existing binding
 */
fun <T, S> Module.bindType(
    bindingType: KClass<*>,
    implementationType: KClass<*>,
    implementationName: String? = null
) {
    withBinding<S>(implementationType, implementationName) { bindType(bindingType) }
}

/** Calls trough [Module.bindName] */
inline fun <reified T> Module.bindName(
    bindingName: String,
    implementationName: String? = null
) {
    bindName<T>(bindingName, T::class, implementationName)
}

/**
 * Adds a binding for [bindingName] to a existing binding
 */
fun <T> Module.bindName(
    bindingName: String,
    implementationType: KClass<*>,
    implementationName: String? = null
) {
    withBinding<T>(implementationType, implementationName) { bindName(bindingName) }
}