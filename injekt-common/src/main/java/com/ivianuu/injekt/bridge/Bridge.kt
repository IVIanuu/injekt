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

package com.ivianuu.injekt.bridge

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.DefinitionContext

import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.get
import com.ivianuu.injekt.typeOf
import java.util.*

inline fun <reified T> ModuleBuilder.bridge(
    name: Qualifier? = null,
    noinline block: (BindingContext<T>.() -> Unit)? = null
): BindingContext<T> = bridge(typeOf(), name, block)

fun <T> ModuleBuilder.bridge(
    type: Type<T>,
    name: Qualifier? = null,
    block: (BindingContext<T>.() -> Unit)? = null
): BindingContext<T> {
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // this binding acts as bridge and just calls trough the original implementation
    return bind(BridgeBinding(type, name), type, UUIDName()).apply {
        block?.invoke(this)
    }
}

private data class UUIDName(private val uuid: String = UUID.randomUUID().toString()) : Qualifier

private class BridgeBinding<T>(
    private val originalType: Type<T>,
    private val originalName: Qualifier?
) : Binding<T> {
    override fun get(context: DefinitionContext, parameters: ParametersDefinition?): T =
        context.get(originalType, originalName)
}