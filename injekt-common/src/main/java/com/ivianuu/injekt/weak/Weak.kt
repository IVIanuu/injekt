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

package com.ivianuu.injekt.weak

import com.ivianuu.injekt.*
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * Weak kind
 */
object WeakKind : Kind() {
    override fun <T> createInstance(
        binding: Binding<T>,
        context: DefinitionContext?
    ): Instance<T> = WeakInstance(binding, context)
    override fun toString() = "Weak"
}

/**
 * Adds a [Binding] which will be created once per [Component]
 */
inline fun <reified T> Module.weak(
    name: Qualifier? = null,
    scope: Scope? = null,
    noinline definition: Definition<T>
) = weak(T::class, name, scope, definition)

/**
 * Adds a [Binding] which will be created once per [Component]
 */
fun <T> Module.weak(
    type: KClass<*>,
    name: Qualifier? = null,
    scope: Scope? = null,
    definition: Definition<T>
) = bind(type, name, WeakKind, scope, definition)

private class WeakInstance<T>(
    override val binding: Binding<T>,
    val defaultContext: DefinitionContext?
) : Instance<T>() {

    private var _value: WeakReference<T>? = null

    override fun get(context: DefinitionContext, parameters: ParametersDefinition?): T {
        val context = defaultContext ?: context

        val value = _value?.get()

        return if (value != null) {
            InjektPlugins.logger?.info("Return existing weak instance $binding")
            value
        } else {
            InjektPlugins.logger?.info("Create weak instance $binding")
            create(context, parameters).also { _value = WeakReference(it) }
        }
    }

}