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

/**
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.DefinitionContext
import com.ivianuu.injekt.DefinitionInstance
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

/**
 * This kind creates no new instances but using a existing one
*/
object ConstantKind : Kind {
override fun <T> createInstance(context: DefinitionContext, binding: Binding<T>): Instance<T> =
ConstantInstance(DefinitionInstance(context, binding))
override fun toString(): String = "Constant"
}

fun <T : Any> Module.constant(
instance: T,
type: Type<T> = typeOf(instance::class),
name: Qualifier? = null,
override: Boolean = false
): Binding<T> = bind(ConstantKind, type, name, override) { instance }

private class ConstantInstance<T>(private val instance: Instance<T>) : Instance<T> {
private val value by lazy(LazyThreadSafetyMode.NONE) {
instance.get()
}

override fun get(parameters: ParametersDefinition?): T {
// todo InjektPlugins.logger?.info("${context.component.scopeName()} Return constant $binding")
return value
}
}*/