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

package com.ivianuu.injekt.eager

import com.ivianuu.injekt.*
import kotlin.reflect.KClass

/**
 * Eager single kind
 */
object EagerSingleKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> =
        EagerSingleInstance(SingleKind.createInstance(binding))

    override fun toString(): String = "EagerSingle"
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
inline fun <reified T> ModuleBuilder.eagerSingle(
    name: Any? = null,
    noinline definition: Definition<T>
) {
    eagerSingle(T::class, name, definition)
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
fun <T> ModuleBuilder.eagerSingle(
    type: KClass<*>,
    name: Any? = null,
    definition: Definition<T>
) {
    bind(type, name, EagerSingleKind, definition)
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
inline fun <reified T> ModuleBuilder.eagerSingleBuilder(
    name: Any? = null,
    noinline definition: Definition<T>? = null,
    noinline block: BindingBuilder<T>.() -> Unit
) {
    eagerSingleBuilder(T::class, name, definition, block)
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
fun <T> ModuleBuilder.eagerSingleBuilder(
    type: KClass<*>,
    name: Any? = null,
    definition: Definition<T>? = null,
    block: BindingBuilder<T>.() -> Unit
) {
    bind(type, name, EagerSingleKind, definition, block)
}

private class EagerSingleInstance<T>(
    private val singleInstance: Instance<T>
) : Instance<T>() {

    override val binding: Binding<T>
        get() = singleInstance.binding

    override fun get(parameters: ParametersDefinition?): T = singleInstance.get(parameters)

    override fun setDefinitionContext(context: DefinitionContext) {
        super.setDefinitionContext(context)
        get(null)
    }
}