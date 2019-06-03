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

import com.ivianuu.injekt.*
import java.util.*

object BridgeKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = BridgeInstance(binding)
    override fun toString(): String = "Bridge"
}

/**
 * Acts as an bridge for an existing [Binding]
 * This allows to add alias bindings and so on to existing bindings
 */
inline fun <reified T> Module.bridge(
    name: Qualifier? = null,
    noinline block: (Binding<T>.() -> Unit)? = null
): Binding<T> = bridge(typeOf(), name, block)

/**
 * Acts as an bridge for an existing [Binding]
 * This allows to add alias bindings and so on to existing bindings
 */
fun <T> Module.bridge(
    type: Type<T>,
    name: Qualifier? = null,
    block: (Binding<T>.() -> Unit)? = null
): Binding<T> {
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // this binding acts as bridge and just calls trough the original implementation
    return bind(
        BridgeKind,
        type,
        UUIDName()
    ) { component.get(type, name) { it } }.apply {
        attribute(ORIGINAL_KEY, Key(type, name))
        block?.invoke(this)
    }
}

private data class UUIDName(private val uuid: String = UUID.randomUUID().toString()) : Qualifier

const val ORIGINAL_KEY = "bridge_original_key"

private class BridgeInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private val originalKey by lazy(LazyThreadSafetyMode.NONE) {
        binding.attributes.get<Key>(ORIGINAL_KEY)
    }

    override fun get(parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("${context.component.scopeName()} Bridge to $originalKey")
        return create(parameters)
    }
}