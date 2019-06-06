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

fun <T> ModuleBuilder.instance(
    instance: T,
    type: Type<T> = typeOf((instance as Any)::class),
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(InstanceBinding(instance, type, name, override))

internal class InstanceBinding<T>(
    private val instance: T,
    type: Type<T>,
    name: Any?,
    override: Boolean
) : Binding<T>(type, name, override) {
    override fun get(parameters: ParametersDefinition?): T = instance
}