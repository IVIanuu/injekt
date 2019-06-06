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
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.DefinitionBinding
import com.ivianuu.injekt.DefinitionContext

import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.SingleBinding

import com.ivianuu.injekt.add
import kotlin.reflect.KClass


inline fun <reified T> ModuleBuilder.eager(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = eager(T::class, name, override, definition)

fun <T> ModuleBuilder.eager(
    type: KClass<*>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> =
    add(EagerBinding(SingleBinding(DefinitionBinding(definition))), type, name, override)

fun <T> Binding<T>.asEagerBinding(): EagerBinding<T> {
    return when {
        this is EagerBinding -> this
        this is SingleBinding -> EagerBinding(this)
        else -> EagerBinding(SingleBinding(this))
    }
}

@Target(AnnotationTarget.CLASS)
annotation class Eager

class EagerBinding<T>(private val binding: Binding<T>) : Binding<T>, AttachAware {

    override fun get(context: DefinitionContext, parameters: ParametersDefinition?) =
        binding.get(context, parameters)

    override fun attached(context: DefinitionContext) {
        get(context)
    }

}