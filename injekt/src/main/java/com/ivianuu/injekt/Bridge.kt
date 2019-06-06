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

import java.util.*

inline fun <reified T> ModuleBuilder.withBinding(
    name: Any? = null,
    noinline block: BindingContext<T>.() -> Unit
) {
    withBinding(typeOf(), name, block)
}

fun <T> ModuleBuilder.withBinding(
    type: Type<T>,
    name: Any? = null,
    block: BindingContext<T>.() -> Unit
) {
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // this binding acts as bridge and just calls trough the original implementation
    bind(BridgeBinding(type, name), type, UUID.randomUUID().toString()).block()
}

private class BridgeBinding<T>(
    private val originalType: Type<T>,
    private val originalName: Any?
) : Binding<T> {
    private lateinit var component: Component
    override fun attach(component: Component) {
        this.component = component
    }

    override fun get(parameters: ParametersDefinition?): T =
        component.get(originalType, originalName)
}