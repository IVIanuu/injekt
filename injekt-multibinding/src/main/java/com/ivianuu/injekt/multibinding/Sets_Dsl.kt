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

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrSet
import com.ivianuu.injekt.withBinding
import kotlin.collections.set
import kotlin.reflect.KClass

/**
 * Declares a empty set binding with the [setName]
 * This is useful for retrieving a [MultiBindingSet] even if no [Binding] was bound into it
 */
fun Module.setBinding(setName: String) {
    factory(name = setName, override = true) {
        MultiBindingSet<Any>(component, emptySet())
    }
}

/**
 * Binds this [Binding] into [setBinding]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setBinding: SetBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_SET_BINDINGS) {
        hashMapOf<String, SetBinding>()
    }[setBinding.setName] = setBinding

    module.declareSetBinding(setBinding.setName)

    return this
}

/**
 * Binds this binding into [setName]
 */
fun <T> BindingContext<T>.bindIntoSet(
    setName: String,
    override: Boolean = false
): BindingContext<T> = bindIntoSet(SetBinding(setName, override))

/**
 * Binds this [Binding] into [setName]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setName: String): BindingContext<T> =
    bindIntoSet(SetBinding(setName))

/**
 * Binds a already existing [Binding] into [setBinding]
 */
inline fun <reified T> Module.bindIntoSet(
    setBinding: SetBinding,
    implementationName: String? = null
) {
    bindIntoSet<T>(T::class, setBinding, implementationName)
}

/**
 * Binds a already existing [Binding] into a [Set] named [setName]
 */
inline fun <reified T> Module.bindIntoSet(
    setName: String,
    override: Boolean = false,
    implementationName: String? = null
) {
    bindIntoSet<T>(SetBinding(setName, override), implementationName)
}

/**
 * Binds a already existing [Binding] into [setBinding]
 */
fun <T> Module.bindIntoSet(
    implementationType: KClass<*>,
    setBinding: SetBinding,
    implementationName: String? = null
) {
    withBinding<T>(implementationType, implementationName) {
        bindIntoSet(setBinding)
        binding.attributes[KEY_ORIGINAL_KEY] = Key(implementationType, implementationName)
    }
}

/**
 * Binds a already existing [Binding] into a [Set] named [setName]
 */
fun <T> Module.bindIntoSet(
    implementationType: KClass<*>,
    setName: String,
    override: Boolean = false,
    implementationName: String? = null
) {
    bindIntoSet<T>(implementationType, SetBinding(setName, override), implementationName)
}