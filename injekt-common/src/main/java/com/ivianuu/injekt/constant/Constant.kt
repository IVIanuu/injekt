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

package com.ivianuu.injekt.constant

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.DefinitionContext
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier

import com.ivianuu.injekt.add
import kotlin.reflect.KClass


fun <T : Any> ModuleBuilder.constant(
    instance: T,
    type: KClass<*> = instance::class,
    name: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = add(ConstantBinding(instance), type, name, override)

private class ConstantBinding<T>(private val instance: T) : Binding<T> {
    override fun get(context: DefinitionContext, parameters: ParametersDefinition?): T = instance
}