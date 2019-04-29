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
 * Eager kind
 */
object EagerKind : Kind() {
    override fun <T> createInstance(
        binding: Binding<T>,
        context: DefinitionContext?
    ): Instance<T> = EagerInstance(binding, context)
    override fun toString(): String = "Eager"
}

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
inline fun <reified T> Module.eager(
    name: Qualifier? = null,
    scope: Scope? = null,
    noinline definition: Definition<T>
): Binding<T> = eager(T::class, name, scope, definition)

/**
 * Adds a [Binding] which will be created once per [Component] and initialized on start
 */
fun <T> Module.eager(
    type: KClass<*>,
    name: Qualifier? = null,
    scope: Scope? = null,
    definition: Definition<T>
): Binding<T> = bind(EagerKind, type, name, scope, definition)

private object UNINITIALIZED

private class EagerInstance<T>(
    override val binding: Binding<T>,
    val defaultContext: DefinitionContext?
) : Instance<T>() {

    private var _value: Any? = UNINITIALIZED

    override fun get(context: DefinitionContext, parameters: ParametersDefinition?): T {
        val context = defaultContext ?: context

        var value = _value
        if (value !== UNINITIALIZED) {
            InjektPlugins.logger?.info("Return existing eager instance $binding")
            return value as T
        }

        synchronized(this) {
            value = _value
            if (value !== UNINITIALIZED) {
                InjektPlugins.logger?.info("Return existing eager instance $binding")
                return@get value as T
            }

            InjektPlugins.logger?.info("Initialize eager instance $binding")
            value = create(context, parameters)
            _value = value
            return@get value as T
        }
    }

}

/**
 * Creates all eager instances
 */
fun Component.createEagerInstances(parameters: ParametersDefinition? = null) {
    bindings
        .filter { it.kind is EagerKind }
        .map { b -> instances.first { i -> i.binding == b } }
        .forEach { it.get(context, parameters) }
}

/**
 * Returns a new [Component] configured by [block] which calls [createEagerInstances]
 */
fun eagerComponent(
    block: (Component.() -> Unit)? = null
): Component {
    return component {
        block?.invoke(this)
        createEagerInstances()
    }
}