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

import com.ivianuu.injekt.AttachAware
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.DefinitionContext
import com.ivianuu.injekt.DefinitionInstance
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.Kind
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.typeOf

/**
 * This kind creates the value once per [Component]
 * and will be initialized on start
 */
object EagerKind : Kind {
    override fun <T> createInstance(context: DefinitionContext, binding: Binding<T>): Instance<T> =
        EagerInstance(DefinitionInstance(context, binding))
    override fun toString(): String = "Eager"
}

inline fun <reified T> Module.eager(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = eager(typeOf(), name, override, definition)

fun <T> Module.eager(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> = bind(EagerKind, type, name, override, definition)

private class EagerInstance<T>(private val instance: Instance<T>) : Instance<T>, AttachAware {

    private var _value: Any? = UNINITIALIZED

    override fun get(parameters: ParametersDefinition?): T {
        var value = _value
        if (value !== UNINITIALIZED) {
            // todo InjektPlugins.logger?.info("${context.component.scopeName()} Return existing eager instance $binding")
            return value as T
        }

        synchronized(this) {
            value = _value
            if (value !== UNINITIALIZED) {
                // todo InjektPlugins.logger?.info("${context.component.scopeName()} Return existing eager instance $binding")
                return@get value as T
            }

            // todo InjektPlugins.logger?.info("${context.component.scopeName()} Initialize eager instance $binding")
            value = instance.get(parameters)
            _value = value
            return@get value as T
        }
    }

    override fun attached() {
        get(null)
    }

    private companion object UNINITIALIZED
}