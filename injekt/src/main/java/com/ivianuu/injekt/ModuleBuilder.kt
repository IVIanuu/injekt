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
import kotlin.reflect.KClass

/**
 * Builder for [Module]s
 */
class ModuleBuilder internal constructor() {

    private val bindings = arrayListOf<Binding<*>>()

    /**
     * Adds the [binding]
     */
    fun <T> addBinding(binding: Binding<T>): BindingContext<T> {
        bindings.add(binding)
        return BindingContext(binding, this)
    }

    /**
     * Returns a new [Module] for this builder
     */
    fun build(): Module = Module(bindings)

}

/**
 * Adds all bindings of the [module]
 */
fun ModuleBuilder.module(module: Module) {
    module.bindings.forEach { addBinding(it) }
}

/** Calls trough [ModuleBuilder.withBinding] */
inline fun <reified T> ModuleBuilder.withBinding(
    name: Any? = null,
    noinline block: BindingContext<T>.() -> Unit
) {
    withBinding(T::class, name, block)
}

/**
 * Invokes the [block] in the [BindingContext] of the [Binding] with [type] and [name]
 */
fun <T> ModuleBuilder.withBinding(
    type: KClass<*>,
    name: Any? = null,
    block: BindingContext<T>.() -> Unit
) {
    // todo this is a little hacky can we turn this into a clean thing?
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // the new factory acts as bridge and just calls trough the original implementation
    addBinding(
        Binding(
            type = type,
            name = UUID.randomUUID().toString(),
            kind = FactoryKind,
            definition = { component.get<T>(type, name) { it } }
        )
    ) withContext block
}