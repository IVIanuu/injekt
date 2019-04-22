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
import kotlin.reflect.KClass

object BridgeKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> =
        BridgeInstance(binding)
    override fun toString(): String = "Bridge"
}

/**
 * Applies the [BridgeKind]
 */
fun BindingBuilder<*>.bridge() {
    kind = BridgeKind
}

/**
 * Acts as an bridge for an existing [Binding]
 * This allows to add alias bindings and so on to existing bindings
 */
inline fun <reified T> ModuleBuilder.bridge(
    name: Any? = null,
    noinline block: BindingBuilder<T>.() -> Unit
) {
    bridge(T::class, name, block)
}

/**
 * Acts as an bridge for an existing [Binding]
 * This allows to add alias bindings and so on to existing bindings
 */
fun <T> ModuleBuilder.bridge(
    type: KClass<*>,
    name: Any? = null,
    block: BindingBuilder<T>.() -> Unit
) {
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // this binding acts as bridge and just calls trough the original implementation
    bind<T>(type, UUID.randomUUID().toString()) {
        bridge()
        definition { get(type, name) { it } }
        block()
        attribute(KEY_ORIGINAL_KEY, Key(type, name))
    }
}

const val KEY_ORIGINAL_KEY = "Bridge.originalKey"

private class BridgeInstance<T>(override val binding: Binding<T>) : Instance<T>() {
    override fun get(parameters: ParametersDefinition?): T = create(parameters)
}