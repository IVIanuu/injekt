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

package com.ivianuu.injekt.multi

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.Kind
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.logger
import com.ivianuu.injekt.scopeName
import com.ivianuu.injekt.typeOf
import kotlin.collections.set

/**
 * This kind creates a values and distinct's them by the hash of the passed [Parameters]
 */
object MultiKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = MultiInstance(binding)
    override fun toString(): String = "Multi"
}

inline fun <reified T> Module.multi(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = multi(typeOf(), name, override, definition)

fun <T> Module.multi(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> = bind(MultiKind, type, name, override, definition)

@Target(AnnotationTarget.CLASS)
annotation class Multi

private class MultiInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private val values = mutableMapOf<Int, T>()

    override fun get(parameters: ParametersDefinition?): T {
        requireNotNull(parameters) { "Parameters cannot be null" }

        val params = parameters()

        val key = params.hashCode()

        var value = values[key]

        return if (value == null && !values.containsKey(key)) {
            InjektPlugins.logger?.info("${context.component.scopeName()} Create multi instance for params $params $binding")
            value = create(parameters)
            values[key] = value
            value
        } else {
            InjektPlugins.logger?.info("${context.component.scopeName()} Return existing multi instance for params $params $binding")
            value as T
        }
    }

}