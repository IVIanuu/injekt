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
 * Adds a [Binding] which will be created on each request
 */
inline fun <reified T> Module.factory(
    name: Qualifier? = null,
    override: Boolean = false,
    unbounded: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = factory(T::class, name, override, unbounded, definition)

/**
 * Adds a [Binding] which will be created on each request
 */
fun <T> Module.factory(
    type: KClass<*>,
    name: Qualifier? = null,
    override: Boolean = false,
    unbounded: Boolean = false,
    definition: Definition<T>
): Binding<T> = bind(FactoryKind, type, name, override, definition)
    .also { it.attribute(KEY_UNBOUNDED, unbounded) }

// todo remove unbounded in favor of unscoped bindings

@Target(AnnotationTarget.CLASS)
annotation class Factory(val scope: KClass<out Scope> = Nothing::class)

const val KEY_UNBOUNDED = "unbounded"

private class FactoryInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private val unbounded: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        binding.attributes.getOrNull<Boolean>(KEY_UNBOUNDED) ?: false
    }

    override fun get(requestingContext: DefinitionContext, parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("Create instance $binding")
        return create(
            if (unbounded) requestingContext else attachedContext ?: requestingContext,
            parameters
        )
    }

}