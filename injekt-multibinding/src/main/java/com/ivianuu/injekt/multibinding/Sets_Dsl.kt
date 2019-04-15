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

package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.*
import kotlin.collections.set

/**
 * Adds this [Binding] into [setBinding]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setBinding: SetBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_SET_BINDINGS) {
        hashMapOf<Name, SetBinding>()
    }[setBinding.setName] = setBinding
    return this
}

/**
 * Adds this binding into [setName]
 */
fun <T> BindingContext<T>.bindIntoSet(
    setName: Name,
    override: Boolean = false
): BindingContext<T> = bindIntoSet(SetBinding(setName, override))

/**
 * Adds this [Binding] into [setName]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setName: Name): BindingContext<T> =
    bindIntoSet(SetBinding(setName))

/**
 * Binds a already existing [Binding] into a [Set] named [setName]
 */
inline fun <reified T> Module.bindIntoSet(
    setName: Name,
    override: Boolean = false,
    implementationName: Name? = null
) {
    bindIntoSet<T>(SetBinding(setName, override), implementationName)
}

/**
 * Binds a already existing [Binding] into [setBinding]
 */
inline fun <reified T> Module.bindIntoSet(
    setBinding: SetBinding,
    implementationName: Name? = null
) {
    withBinding<T>(implementationName) {
        bindIntoSet(setBinding)
        binding.attributes[KEY_ORIGINAL_KEY] = Key(T::class, implementationName)
    }
}