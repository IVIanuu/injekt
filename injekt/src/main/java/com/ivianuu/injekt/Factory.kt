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
 * Kind for factory instances
 */
object FactoryKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = FactoryInstance(binding)
    override fun toString(): String = "Factory"
}

/**
 * Applies the [FactoryKind]
 */
fun BindingBuilder<*>.factory() {
    kind = FactoryKind
}

/**
 * Adds a [Binding] which will be created on each request
 */
inline fun <reified T> ModuleBuilder.factory(
    name: Any? = null,
    noinline definition: Definition<T>
) {
    factory(T::class, name, definition)
}

/**
 * Adds a [Binding] which will be created on each request
 */
fun <T> ModuleBuilder.factory(
    type: KClass<*>,
    name: Any? = null,
    definition: Definition<T>
) {
    bind(type, name, FactoryKind, definition)
}

/**
 * Adds a [Binding] which will be created on each request
 */
inline fun <reified T> ModuleBuilder.factoryBuilder(
    name: Any? = null,
    noinline definition: Definition<T>? = null,
    noinline block: BindingBuilder<T>.() -> Unit
) {
    factoryBuilder(T::class, name, definition, block)
}

/**
 * Adds a [Binding] which will be created on each request
 */
fun <T> ModuleBuilder.factoryBuilder(
    type: KClass<*>,
    name: Any? = null,
    definition: Definition<T>? = null,
    block: BindingBuilder<T>.() -> Unit
) {
    bind(type, name, FactoryKind, definition, block)
}

private class FactoryInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    override fun get(parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("Create instance $binding")
        return create(parameters)
    }

}