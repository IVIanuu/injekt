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

import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.UnlinkedBinding
import com.ivianuu.injekt.add
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
    return add(UnlinkedBridgeBinding(type, name), type, UUIDName()).apply {
        block?.invoke(this)
    }
}

private data class UUIDName(private val uuid: String = UUID.randomUUID().toString()) : Qualifier

private class UnlinkedBridgeBinding<T>(
    private val originalType: Type<T>,
    private val originalName: Qualifier?
) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        LinkedBridgeBinding(linker.get(originalType, originalName))
}

private class LinkedBridgeBinding<T>(
    private val originalBinding: LinkedBinding<T>
) : LinkedBinding<T>() {
    override fun get(parameters: ParametersDefinition?): T = originalBinding(parameters)
}