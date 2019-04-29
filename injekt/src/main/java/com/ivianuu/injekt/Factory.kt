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
    override fun <T> createInstance(
        binding: Binding<T>,
        context: DefinitionContext?
    ): Instance<T> = FactoryInstance(binding, context)
    override fun toString(): String = "Factory"
}

/**
 * Adds a [Binding] which will be created on each request
 */
inline fun <reified T> Module.factory(
    name: Qualifier? = null,
    scope: Scope? = null,
    noinline definition: Definition<T>
) = factory(T::class, name, scope, definition)

/**
 * Adds a [Binding] which will be created on each request
 */
fun <T> Module.factory(
    type: KClass<*>,
    name: Qualifier? = null,
    scope: Scope? = null,
    definition: Definition<T>
) = bind(type, name, FactoryKind, scope, definition)

private class FactoryInstance<T>(
    override val binding: Binding<T>,
    val defaultContext: DefinitionContext?
) : Instance<T>() {

    override fun get(
        context: DefinitionContext,
        parameters: ParametersDefinition?
    ): T {
        val context = defaultContext ?: context
        InjektPlugins.logger?.info("Create instance $binding")
        return create(context, parameters)
    }

}