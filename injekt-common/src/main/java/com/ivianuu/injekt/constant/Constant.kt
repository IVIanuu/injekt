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

package com.ivianuu.injekt.constant

import com.ivianuu.injekt.*
import kotlin.reflect.KClass

/**
 * Constant instance kind
 */
object ConstantKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> =
        ConstantInstance(binding)

    override fun toString(): String = "Constant"
}

/**
 * Applies the [ConstantKind]
 */
fun BindingBuilder<*>.constant() {
    kind = ConstantKind
}

/**
 * Adds a [Binding] which already exists
 */
fun <T : Any> Module.constant(
    instance: T,
    type: KClass<*> = instance::class,
    name: Any? = null,
    override: Boolean = false
) {
    bind(type, name, ConstantKind, override, { instance })
}

/**
 * Adds a [Binding] which already exists
 */
fun <T : Any> Module.constantBuilder(
    instance: T,
    type: KClass<*> = instance::class,
    name: Any? = null,
    override: Boolean = false,
    block: BindingBuilder<T>.() -> Unit
) {
    bind(type, name, ConstantKind, override, { instance }, block)
}

private class ConstantInstance<T>(override val binding: Binding<T>) : Instance<T>() {
    override fun get(parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("Return constant $binding")
        return create(parameters)
    }
}