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

import com.ivianuu.injekt.*

/**
 * This kind creates no new instances but using a existing one
 */
object ConstantKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = ConstantInstance(binding)
    override fun toString(): String = "Constant"
}

fun <T : Any> Module.constant(
    instance: T,
    type: Type<T> = typeOf(instance::class),
    name: Qualifier? = null,
    override: Boolean = false
): Binding<T> = bind(ConstantKind, type, name, override) { instance }

private class ConstantInstance<T>(override val binding: Binding<T>) : Instance<T>() {
    private val instance by lazy(LazyThreadSafetyMode.NONE) {
        binding.definition(context, emptyParameters())
    }

    override fun get(parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("${context.component.scopeName()} Return constant $binding")
        return instance
    }
}