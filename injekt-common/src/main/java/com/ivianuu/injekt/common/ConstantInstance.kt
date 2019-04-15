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

import com.ivianuu.injekt.*

/**
 * Constant instance kind
 */
object ConstantKind : Kind {

    private const val INSTANCE_KIND = "Constant"

    override fun <T> createInstance(binding: Binding<T>, context: DefinitionContext?): Instance<T> =
        ConstantInstance(binding)

    override fun asString(): String = INSTANCE_KIND

}

/**
 * Holds a constant instance
 */
class ConstantInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    override fun get(context: DefinitionContext, parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("Return constant $binding")
        return create(context, parameters)
    }

}

/**
 * Provides a constant instance
 */
inline fun <reified T> Module.constant(
    name: Name? = null,
    override: Boolean = false,
    crossinline instance: () -> T
): BindingContext<T> = add(
    Binding(
        type = T::class,
        name = name,
        kind = ConstantKind,
        override = override,
        definition = { instance() }
    )
)

/**
 * Adds a [Binding] for the [instance]
 */
fun <T : Any> Component.addConstant(instance: T) {
    addBinding(
        Binding(
            type = instance::class,
            kind = ConstantKind,
            definition = { instance }
        )
    )
}